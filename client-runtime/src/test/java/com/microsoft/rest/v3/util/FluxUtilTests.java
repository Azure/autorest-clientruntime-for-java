/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v3.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.Exceptions;
import reactor.test.StepVerifier;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

public class FluxUtilTests {

    @Test
    public void testCanReadSlice() throws IOException {
        File file = new File("target/test1");
        FileOutputStream stream = new FileOutputStream(file);
        stream.write("hello there".getBytes(StandardCharsets.UTF_8));
        try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            byte[] bytes = FluxUtil.byteBufferStreamFromFile(channel, 1, 3)
                    .map(bb -> toBytes(bb))
                    .collect(() -> new ByteArrayOutputStream(),
                        (bos, b) -> {
                            try {
                                bos.write(b);
                            } catch (IOException ioe) {
                                throw Exceptions.propagate(ioe);
                            }
                        })
                    .block()
                    .toByteArray();
            assertEquals("ell", new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException ioe) {

        }

    }

    @Test
    public void testCanReadEmptyFile() throws IOException {
        File file = new File("target/test2");
        file.createNewFile();
        try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            byte[] bytes = FluxUtil.byteBufferStreamFromFile(channel, 1, 3)
                    .map(bb -> toBytes(bb)) //
                    .collect(() -> new ByteArrayOutputStream(),
                            (bos, b) -> {
                                try {
                                    bos.write(b);
                                } catch (IOException ioe) {
                                    throw Exceptions.propagate(ioe);
                                }
                            })
                    .block().toByteArray();
            assertEquals(0, bytes.length);
        }
        assertTrue(file.delete());
    }

    @Test
    public void testAsynchronyShortInput() throws IOException {
        File file = new File("target/test3");
        FileOutputStream stream = new FileOutputStream(file);
        stream.write("hello there".getBytes(StandardCharsets.UTF_8));
        try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            byte[] bytes = FluxUtil.byteBufferStreamFromFile(channel)
                    .map(bb -> toBytes(bb))
                    .limitRequest(1)    // TODO: Check with smaldini - With Rx rebatchRequests(1) was used, what is equivalent in reactor
                    .subscribeOn(reactor.core.scheduler.Schedulers.newElastic("io", 30))
                    .publishOn(reactor.core.scheduler.Schedulers.newElastic("io", 30))
                    .collect(() -> new ByteArrayOutputStream(),
                            (bos, b) -> {
                                try {
                                    bos.write(b);
                                } catch (IOException ioe) {
                                    throw Exceptions.propagate(ioe);
                                }
                            })
                    .block()
                    .toByteArray();
            assertEquals("hello there", new String(bytes, StandardCharsets.UTF_8));
        }
        assertTrue(file.delete());
    }

    private static final int NUM_CHUNKS_IN_LONG_INPUT = 10_000_000;

    @Test
    public void testAsynchronyLongInput() throws IOException, NoSuchAlgorithmException {
        File file = new File("target/test4");
        byte[] array = "1234567690".getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            for (int i = 0; i < NUM_CHUNKS_IN_LONG_INPUT; i++) {
                out.write(array);
                digest.update(array);
            }
        }
        System.out.println("long input file size="+ file.length()/(1024*1024) + "MB");
        byte[] expected = digest.digest();
        digest.reset();
        try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            FluxUtil.byteBufferStreamFromFile(channel)
                    .subscribeOn(reactor.core.scheduler.Schedulers.newElastic("io", 30))
                    .publishOn(reactor.core.scheduler.Schedulers.newElastic("io", 30))
                    .toIterable().forEach(bb -> digest.update(bb));

            assertArrayEquals(expected, digest.digest());
        }
        assertTrue(file.delete());
    }

      @Test
      @Ignore("Need to sync with smaldini to find equivalent for rx.test.awaitDone")
      public void testBackpressureLongInput() throws IOException, NoSuchAlgorithmException {
//        File file = new File("target/test4");
//        byte[] array = "1234567690".getBytes(StandardCharsets.UTF_8);
//        MessageDigest digest = MessageDigest.getInstance("MD5");
//        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
//            for (int i = 0; i < NUM_CHUNKS_IN_LONG_INPUT; i++) {
//                out.write(array);
//                digest.update(array);
//            }
//        }
//        byte[] expected = digest.digest();
//        digest.reset();
//
//        try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ)) {
//            FluxUtil1.byteBufferStreamFromFile(channel)
//                    .rebatchRequests(1)
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(Schedulers.io())
//                    .doOnNext(bb -> digest.update(bb))
//                    .test(0)
//                    .assertNoValues()
//                    .requestMore(1)
//                    .awaitCount(1)
//                    .assertValueCount(1)
//                    .requestMore(1)
//                    .awaitCount(2)
//                    .assertValueCount(2)
//                    .requestMore(Long.MAX_VALUE)
//                    .awaitDone(20, TimeUnit.SECONDS)
//                    .assertComplete();
//        }
//
//        assertArrayEquals(expected, digest.digest());
//        assertTrue(file.delete());
      }
    
    @Test
    public void testSplitForMultipleSplitSizesFromOneTo16() throws NoSuchAlgorithmException {
        ByteBuf bb = null;
        try {
            bb = Unpooled.directBuffer(1000);
            byte [] oneByte = new byte[1];
            for (int i = 0; i < 1000; i++) {
                oneByte[0] = (byte) i;
                bb.writeBytes(oneByte);
            }
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(bb.nioBuffer());
            byte[] expected = digest.digest();
            for (int size = 1; size < 16; size++) {
                System.out.println("size=" + size);
                digest.reset();
                bb.readerIndex(0);
                //
                FluxUtil.split(bb, 3).doOnNext(b -> digest.update(b.nioBuffer()))
                        .subscribe();
//
//            StepVerifier.create(FluxUtil1.split(bb, 3).doOnNext(b -> digest.update(b)))
//                    .expectNextCount(?) // TODO: ? is Unknown. Check with smaldini - what is the Verifier way to ignore all next calls and simply check stream completes?
//                    .verifyComplete();
//
                assertArrayEquals(expected, digest.digest());
            }
        } finally {
            if (bb != null) {
                bb.release();
            }
        }
    }
    
    @Test
    public void testSplitOnEmptyContent() {
        ByteBuf bb = null;
        try {
            bb = Unpooled.directBuffer(16);
            StepVerifier.create(FluxUtil.split(bb, 3))
                    .expectNextCount(0)
                    .expectComplete()
                    .verify();
        } finally {
            if (bb != null) {
                bb.release();
            }
        }
    }
//
    private static byte[] toBytes(ByteBuffer bb) {
        byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        return bytes;
    }

}
