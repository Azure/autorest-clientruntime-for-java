/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.http;

import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an asynchronous input stream with a content length.
 */
public final class AsyncInputStream {
    private static final int CHUNK_SIZE = 8192;
    private final Flowable<byte[]> content;
    private final long contentLength;
    private final boolean isReplayable;

    /**
     * Creates an AsyncInputStream.
     * @param flowable The flowable which emits the stream content.
     * @param contentLength The total length of the stream content.
     * @param isReplayable indicates whether the flowable allows multiple subscription.
     *                     Used as a hint for whether to buffer flowable content when retrying.
     */
    public AsyncInputStream(Flowable<byte[]> flowable, long contentLength, boolean isReplayable) {
        this.content = flowable;
        this.contentLength = contentLength;
        this.isReplayable = isReplayable;
    }

    /**
     * @return The flowable which emits the stream content.
     */
    public Flowable<byte[]> content() {
        return content;
    }

    /**
     * @return The total length of the stream content.
     */
    public long contentLength() {
        return contentLength;
    }

    /**
     * @return a value indicating whether the content Flowable contained
     *         in this AsyncInputStream supports multiple subscription
     */
    public boolean isReplayable() {
        return isReplayable;
    }

    /**
     * Creates an AsyncInputStream from an AsynchronousFileChannel.
     *
     * @param fileChannel The file channel.
     * @param offset The offset in the file to begin reading.
     * @param length The number of bytes of data to read from the file.
     * @return The AsyncInputStream.
     */
    public static AsyncInputStream create(final AsynchronousFileChannel fileChannel, final long offset, final long length) {
        if (true) {
            return new AsyncInputStream(new FlowableNio2(fileChannel, offset, length), length, true);
        }

        int numChunks = (int) length / CHUNK_SIZE + (length % CHUNK_SIZE == 0 ? 0 : 1);
        Flowable<byte[]> fileStream = Flowable.range(0, numChunks).concatMap(new Function<Integer, Flowable<byte[]>>() {
            ByteBuffer innerBuf = ByteBuffer.wrap(new byte[CHUNK_SIZE]);

            @Override
            public Flowable<byte[]> apply(Integer chunkNo) throws Exception {
                final long position = offset + (chunkNo * CHUNK_SIZE);
                innerBuf.clear();
                return Flowable.fromFuture(fileChannel.read(innerBuf, position))
                        .map(new Function<Integer, byte[]>() {
                            @Override
                            public byte[] apply(Integer bytesRead) throws Exception {
                                int bytesWanted = (int) Math.min(offset + length - position, bytesRead);
                                return Arrays.copyOf(innerBuf.array(), bytesWanted);
                            }
                        });
            }
        });

        return new AsyncInputStream(fileStream, length, true);
    }

    /**
     * Creates an AsyncInputStream from an AsynchronousFileChannel which reads the entire file.
     * @param fileChannel The file channel.
     * @throws IOException if an error occurs when determining file size
     * @return The AsyncInputStream.
     */
    public static AsyncInputStream create(AsynchronousFileChannel fileChannel) throws IOException {
        long size = fileChannel.size();
        return create(fileChannel, 0, size);
    }

    private static class FlowableNio2 extends Flowable<byte[]> {
        private final AsynchronousFileChannel fileChannel;
        private final long offset;
        private final long length;

        FlowableNio2(AsynchronousFileChannel fileChannel, long offset, long length) {
            this.fileChannel = fileChannel;
            this.offset = offset;
            this.length = length;
        }

        @Override
        protected void subscribeActual(Subscriber<? super byte[]> s) {
            s.onSubscribe(new Nio2Subscription(s));
        }

        private class Nio2Subscription implements Subscription {
            final Subscriber<? super byte[]> subscriber;
            final ByteBuffer innerBuf = ByteBuffer.wrap(new byte[8192]);
            final AtomicLong requested = new AtomicLong();
            long position = offset;
            volatile boolean cancelled = false;

            Nio2Subscription(Subscriber<? super byte[]> subscriber) {
                this.subscriber = subscriber;
            }

            @Override
            public void request(long n) {
                if (BackpressureHelper.add(requested, n) == 0L) {
                    doRead();
                }
            }

            void doRead() {
                innerBuf.clear();
                fileChannel.read(innerBuf, position, null, onReadComplete);
            }

            CompletionHandler<Integer, Object> onReadComplete = new CompletionHandler<Integer, Object>() {
                @Override
                public void completed(Integer bytesRead, Object attachment) {
                    if (!cancelled) {
                        if (bytesRead == -1) {
                            subscriber.onComplete();
                        } else {
                            long remaining = requested.decrementAndGet();
                            int bytesWanted = (int) Math.min(CHUNK_SIZE, offset + length - position);
                            subscriber.onNext(Arrays.copyOf(innerBuf.array(), bytesWanted));
                            position += bytesRead;
                            if (position >= offset + length) {
                                subscriber.onComplete();
                            } else if (remaining > 0) {
                                doRead();
                            }
                        }
                    }
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    if (!cancelled) {
                        subscriber.onError(exc);
                    }
                }
            };

            @Override
            public void cancel() {
                cancelled = true;
            }
        }
    }

    /**
     * Creates an AsyncInputStream which emits the content of a given InputStream with a known length.
     *
     * @param inputStream The input stream.
     * @param contentLength The length of the stream content.
     * @return An AsyncInputStream which emits the content from the given InputStream.
     */
    public static AsyncInputStream create(final InputStream inputStream, long contentLength) {
        Flowable<byte[]> content = Flowable.generate(
                new Callable<InputStream>() {
                    @Override
                    public InputStream call() throws Exception {
                        return inputStream;
                    }
                },
                new BiConsumer<InputStream, Emitter<byte[]>>() {
                    private final byte[] innerBuf = new byte[CHUNK_SIZE];

                    @Override
                    public void accept(InputStream inputStream, Emitter<byte[]> emitter) throws Exception {
                        try {
                            int bytesRead = inputStream.read(innerBuf);
                            if (bytesRead == -1) {
                                emitter.onComplete();
                            } else {
                                byte[] nextBuf = Arrays.copyOf(innerBuf, bytesRead);
                                emitter.onNext(nextBuf);
                            }
                        } catch (IOException e) {
                            emitter.onError(e);
                        }
                    }
                }).subscribeOn(Schedulers.io());

        return new AsyncInputStream(content, contentLength, false);
    }

    /**
     * Creates an AsyncInputStream which emits the given byte array.
     * @param bytes the bytes to emit in the stream
     * @return the AsyncInputStream
     */
    public static AsyncInputStream create(byte[] bytes) {
        return new AsyncInputStream(Flowable.just(bytes), bytes.length, true);
    }
}
