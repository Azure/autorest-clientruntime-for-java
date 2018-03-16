/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * An HttpClient that is implemented using Netty.
 */
public final class NettyClient extends HttpClient {
    private final NettyAdapter adapter;
    private final Proxy proxy;

    /**
     * Creates NettyClient.
     * @param configuration the HTTP client configuration.
     * @param adapter the adapter to Netty
     */
    private NettyClient(HttpClientConfiguration configuration, NettyAdapter adapter) {
        this.adapter = adapter;
        this.proxy = configuration == null ? null : configuration.proxy();
    }

    @Override
    public Single<HttpResponse> sendRequestAsync(final HttpRequest request) {
        return adapter.sendRequestInternalAsync(request, proxy);
    }

    private static final class NettyAdapter {
        private static final String EPOLL_GROUP_CLASS_NAME = "io.netty.channel.epoll.EpollEventLoopGroup";
        private static final String EPOLL_SOCKET_CLASS_NAME = "io.netty.channel.epoll.EpollSocketChannel";

        private static final String KQUEUE_GROUP_CLASS_NAME = "io.netty.channel.kqueue.KQueueEventLoopGroup";
        private static final String KQUEUE_SOCKET_CLASS_NAME = "io.netty.channel.kqueue.KQueueSocketChannel";

        private final MultithreadEventLoopGroup eventLoopGroup;
        private final SharedChannelPool channelPool;

        public Future<?> shutdownGracefully() {
            channelPool.close();
            return eventLoopGroup.shutdownGracefully();
        }

        private static final class TransportConfig {
            final MultithreadEventLoopGroup eventLoopGroup;
            final Class<? extends SocketChannel> channelClass;

            private TransportConfig(MultithreadEventLoopGroup eventLoopGroup, Class<? extends SocketChannel> channelClass) {
                this.eventLoopGroup = eventLoopGroup;
                this.channelClass = channelClass;
            }
        }

        private static MultithreadEventLoopGroup loadEventLoopGroup(String className, int size) throws ReflectiveOperationException {
            Class<?> cls = Class.forName(className);
            ThreadFactory factory = new DefaultThreadFactory(cls, true);
            MultithreadEventLoopGroup result = (MultithreadEventLoopGroup) cls.getConstructor(Integer.TYPE, ThreadFactory.class).newInstance(size, factory);
            return result;
        }

        @SuppressWarnings("unchecked")
        private static TransportConfig loadTransport(int groupSize) {
            TransportConfig result = null;
            try {
                final String osName = System.getProperty("os.name");
                if (osName.contains("Linux")) {
                    result = new TransportConfig(
                            loadEventLoopGroup(EPOLL_GROUP_CLASS_NAME, groupSize),
                            (Class<? extends SocketChannel>) Class.forName(EPOLL_SOCKET_CLASS_NAME));
                } else if (osName.contains("Mac")) {
                    result = new TransportConfig(
                            loadEventLoopGroup(KQUEUE_GROUP_CLASS_NAME, groupSize),
                            (Class<? extends SocketChannel>) Class.forName(KQUEUE_SOCKET_CLASS_NAME));
                }
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null) {
                    Throwable cause = e.getCause();
                    if (cause != null) {
                        message = cause.getMessage();
                    }
                }
                LoggerFactory.getLogger(NettyAdapter.class).debug("Exception when obtaining native EventLoopGroup and SocketChannel: " + message);
            }

            if (result == null) {
                result = new TransportConfig(new NioEventLoopGroup(groupSize, new DefaultThreadFactory(NioEventLoopGroup.class, true)), NioSocketChannel.class);
            }

            return result;
        }

        private static SharedChannelPool createChannelPool(final NettyAdapter adapter, TransportConfig config, int poolSize) {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(config.eventLoopGroup);
            bootstrap.channel(config.channelClass);
            bootstrap.option(ChannelOption.AUTO_READ, false);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.MINUTES.toMillis(3L));
            return new SharedChannelPool(bootstrap, new AbstractChannelPoolHandler() {
                @Override
                public void channelCreated(Channel ch) throws Exception {
                    ch.pipeline().addLast("HttpResponseDecoder", new HttpResponseDecoder());
                    ch.pipeline().addLast("HttpRequestEncoder", new HttpRequestEncoder());
                    ch.pipeline().addLast("HttpClientInboundHandler", new HttpClientInboundHandler(adapter));
                }
            }, poolSize);
        }

        private NettyAdapter() {
            TransportConfig config = loadTransport(0);
            this.eventLoopGroup = config.eventLoopGroup;
            this.channelPool = createChannelPool(this, config, eventLoopGroup.executorCount() * 16);
        }

        private NettyAdapter(int eventLoopGroupSize, int channelPoolSize) {
            TransportConfig config = loadTransport(eventLoopGroupSize);
            this.eventLoopGroup = config.eventLoopGroup;
            this.channelPool = createChannelPool(this, config, channelPoolSize);
        }

        private Single<HttpResponse> sendRequestInternalAsync(final HttpRequest request, final Proxy proxy) {
            final URI channelAddress;
            try {
                if (proxy == null) {
                    channelAddress = request.url().toURI();
                } else if (proxy.address() instanceof InetSocketAddress) {
                    InetSocketAddress address = (InetSocketAddress) proxy.address();
                    String scheme = address.getPort() == 443
                            ? "https"
                            : "http";

                    String channelAddressString = scheme + "://" + address.getHostString() + ":" + address.getPort();
                    channelAddress = new URI(channelAddressString);
                } else {
                    throw new IllegalArgumentException(
                            "SocketAddress on java.net.Proxy must be an InetSocketAddress. Found proxy: " + proxy);
                }

                request.withHeader(io.netty.handler.codec.http.HttpHeaderNames.HOST.toString(), request.url().getHost());
                request.withHeader(io.netty.handler.codec.http.HttpHeaderNames.CONNECTION.toString(), io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE.toString());
            } catch (URISyntaxException e) {
                return Single.error(e);
            }

            // Creates cold observable from an emitter
            return Single.create((SingleEmitter<HttpResponse> responseEmitter) -> {
                channelPool.acquire(channelAddress).addListener(new GenericFutureListener<Future<? super Channel>>() {
                    private void emitErrorIfSubscribed(Throwable throwable) {
                        if (!responseEmitter.isDisposed()) {
                            responseEmitter.onError(throwable);
                        }
                    }

                    @Override
                    public void operationComplete(Future<? super Channel> cf) {
                        if (!cf.isSuccess()) {
                            emitErrorIfSubscribed(cf.cause());
                            return;
                        }

                        final Channel channel = (Channel) cf.getNow();
                        final HttpClientInboundHandler inboundHandler = channel.pipeline().get(HttpClientInboundHandler.class);

                        if (responseEmitter.isDisposed()) {
                            // We were cancelled before sending any data, so just return the channel to the pool.
                            channelPool.release(channel);
                            return;
                        }

                        // After this point, we're starting to send data, so if the Single<HttpResponse> gets canceled we need to close the channel.
                        inboundHandler.didEmitHttpResponse = false;
                        inboundHandler.responseEmitter = responseEmitter;
                        responseEmitter.setDisposable(new Disposable() {
                            boolean isDisposed = false;
                            @Override
                            public void dispose() {
                                isDisposed = true;
                                if (!inboundHandler.didEmitHttpResponse) {
                                    channelPool.closeAndRelease(channel);
                                }
                            }

                            @Override
                            public boolean isDisposed() {
                                return isDisposed;
                            }
                        });

                        if (request.httpMethod() == com.microsoft.rest.v2.http.HttpMethod.HEAD) {
                            // Use HttpClientCodec for HEAD operations
                            if (channel.pipeline().get("HttpClientCodec") == null) {
                                channel.pipeline().remove(HttpRequestEncoder.class);
                                channel.pipeline().replace(HttpResponseDecoder.class, "HttpClientCodec", new HttpClientCodec());
                            }
                        } else {
                            // Use HttpResponseDecoder for other operations
                            if (channel.pipeline().get("HttpResponseDecoder") == null) {
                                channel.pipeline().replace(HttpClientCodec.class, "HttpResponseDecoder", new HttpResponseDecoder());
                                channel.pipeline().addAfter("HttpResponseDecoder", "HttpRequestEncoder", new HttpRequestEncoder());
                            }
                        }

                        final DefaultHttpRequest raw = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                                HttpMethod.valueOf(request.httpMethod().toString()),
                                request.url().toString());

                        for (HttpHeader header : request.headers()) {
                            raw.headers().add(header.name(), header.value());
                        }

                        try {
                            channel.write(raw).addListener((Future<Void> future) -> {
                                if (!future.isSuccess()) {
                                    channelPool.closeAndRelease(channel);
                                    emitErrorIfSubscribed(future.cause());
                                }
                            });

                            if (request.body() == null) {
                                channel.writeAndFlush(DefaultLastHttpContent.EMPTY_LAST_CONTENT)
                                        .addListener((Future<Void> future) -> {
                                            if (future.isSuccess()) {
                                                channel.read();
                                            } else {
                                                channelPool.closeAndRelease(channel);
                                                emitErrorIfSubscribed(future.cause());
                                            }
                                        });
                            } else {
                                request.body().observeOn(Schedulers.from(channel.eventLoop())).subscribe(new FlowableSubscriber<ByteBuffer>() {
                                    Subscription subscription;
                                    @Override
                                    public void onSubscribe(Subscription s) {
                                        subscription = s;
                                        inboundHandler.requestContentSubscription = subscription;
                                        subscription.request(1);
                                    }

                                    GenericFutureListener<Future<Void>> onChannelWriteComplete = (Future<Void> future) -> {
                                        if (!future.isSuccess()) {
                                            subscription.cancel();
                                            channelPool.closeAndRelease(channel);
                                            emitErrorIfSubscribed(future.cause());
                                        }
                                    };

                                    @Override
                                    public void onNext(ByteBuffer buf) {
                                        if (!channel.eventLoop().inEventLoop()) {
                                            throw new IllegalStateException("onNext must be called from the event loop managing the channel.");
                                        }
                                        try {
                                            channel.writeAndFlush(Unpooled.wrappedBuffer(buf))
                                                    .addListener(onChannelWriteComplete);

                                            if (channel.isWritable()) {
                                                subscription.request(1);
                                            }
                                        } catch (Exception e) {
                                            subscription.cancel();
                                            emitErrorIfSubscribed(e);
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable t) {
                                        channelPool.closeAndRelease(channel);
                                        emitErrorIfSubscribed(t);
                                    }

                                    @Override
                                    public void onComplete() {
                                        try {
                                            channel.writeAndFlush(DefaultLastHttpContent.EMPTY_LAST_CONTENT)
                                                    .addListener((Future<Void> future) -> {
                                                        if (!future.isSuccess()) {
                                                            channelPool.closeAndRelease(channel);
                                                            emitErrorIfSubscribed(future.cause());
                                                        } else {
                                                            channel.read();
                                                        }
                                                    });
                                        } catch (Exception e) {
                                            emitErrorIfSubscribed(e);
                                        }
                                    }
                                });
                            }
                        } catch (Exception e) {
                            emitErrorIfSubscribed(e);
                        }
                    }
                });
            }).onErrorResumeNext((Throwable throwable) -> {
                if (throwable instanceof EncoderException) {
                    LoggerFactory.getLogger(getClass()).warn("Got EncoderException: " + throwable.getMessage());
                    return sendRequestInternalAsync(request, proxy);
                } else {
                    return Single.error(throwable);
                }
            });
        }
    }


    /**
     * Emits HTTP response content from Netty.
     */
    private static final class ResponseContentFlowable extends Flowable<ByteBuf>
            implements Subscription {

        // single producer, single consumer queue
        private final SimplePlainQueue<HttpContent> queue = new SpscLinkedArrayQueue<>(16);
        private final Subscription channelSubscription;
        private final AtomicBoolean chunkRequested = new AtomicBoolean();
        private final AtomicLong requested = new AtomicLong();
        
        // work-in-progress counter
        private final AtomicInteger wip = new AtomicInteger(1); //set to 1 to disable drain till we are ready
        
        // ensures one subscriber only
        private final AtomicBoolean once = new AtomicBoolean();
        
        private Subscriber<? super ByteBuf> subscriber;
        
        // can be non-volatile because event methods onReceivedContent, 
        // chunkComplete, onError are serialized and is only written and
        // read in those event methods
        private boolean done;

        private volatile boolean cancelled = false;

        // must be volatile otherwise parts of Throwable might not be visible to drain
        // loop (or suffer from word tearing)
        private volatile Throwable err;

        ResponseContentFlowable(Subscription channelSubscription) {
            this.channelSubscription = channelSubscription;
        }

        @Override
        protected void subscribeActual(Subscriber<? super ByteBuf> s) {
            if (once.compareAndSet(false, true)) {
                subscriber = s;
                // now that subscriber has been set enable the drain loop
                wip.lazySet(0);
                
                s.onSubscribe(this);
            } else {
                s.onSubscribe(SubscriptionHelper.CANCELLED);
                s.onError(new IllegalStateException(
                        "Multiple subscriptions not allowed for response content"));
            }
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            channelSubscription.cancel();
            drain();
        }

        //
        // EVENTS - serialized
        //

        void onReceivedContent(HttpContent data) {
            if (done) {
                RxJavaPlugins.onError(new IllegalStateException("data arrived after LastHttpContent"));
                return;
            }
            if (data instanceof LastHttpContent) {
                done = true;
            }
            if (cancelled) {
                data.release();
            } else {
                queue.offer(data);
                drain();
            }
        }

        void chunkCompleted() {
            if (done) {
                RxJavaPlugins.onError(new IllegalStateException("chunk completion event occurred after done is true"));
                return;
            }
            chunkRequested.set(false);
            drain();
        }

        void onError(Throwable cause) {
            if (done) {
                RxJavaPlugins.onError(cause);
            }
            done = true;
            err = cause;
            drain();
        }
        
        void channelInactive() {
            if (!done) {
                done = true;
                err = new IOException("channel inactive");
                drain();
            }
        }

        //
        // PRIVATE METHODS
        //

        private void requestChunkOfByteBufsFromUpstream() {
            channelSubscription.request(1);
        }
        
        private void drain() {
            // Below is a non-blocking technique to ensure serialization (in-order processing) of the block inside the if statement
            // and also to ensure no race conditions exist where items on the queue would be missed.
            //
            // wip = `work in progress` and follows a naming convention in RxJava
            // We want to ensure that if items are added to the queue and `drain` is called while it is running that there is no race
            // condition where the items are not picked up.
            //
            // `missed` is a clever little trick to ensure that we only do as many loops as actually required. If `drain` is called 
            // say 10 times while the `drain` loop is active then we notice that there are possibly extra items on the queue that arrived
            // just after we found none left (and before the method exits). We don't need to loop around ten times but only once because
            // all items will be picked up from the queue in one additional polling loop. 
            if (wip.getAndIncrement() == 0) {
                // need to check cancelled even if there are no requests
                if (cancelled) {
                    releaseQueue();
                    return;
                }
                int missed = 1;
                long r = requested.get();
                while (true) {
                    long e = 0;
                    while (e != r) {
                        // Note that an error can shortcut the emission of content that is currently on the queue.
                        // This is probably desirable generally because it prevents work that being done downstream 
                        // that might be thrown away anyway due to the error. 
                        Throwable error = err;
                        if (error != null) {
                            releaseQueue();
                            channelSubscription.cancel();
                            subscriber.onError(error);
                            return;
                        }
                        HttpContent o = queue.poll();
                        if (o != null) {
                            e++;
                            if (emitContent(o)) {
                                return;
                            }
                        } else {
                            // queue is empty so lets see if we need to request another chunk
                            // note that we can only request one chunk at a time because the 
                            // method channel.read() ignores calls if a read is pending
                            if (chunkRequested.compareAndSet(false, true)) {
                                requestChunkOfByteBufsFromUpstream();
                            }
                            break;
                        }
                        if (cancelled) {
                            releaseQueue();
                            return;
                        }
                    }
                    if (e > 0) {
                        r = BackpressureHelper.produced(requested, e);
                    }
                    missed = wip.addAndGet(-missed);
                    if (missed == 0) {
                        return;
                    }
                }
            }
        }

        // should only be called from the drain loop
        // returns true if complete
        private boolean emitContent(HttpContent data) {
            subscriber.onNext(data.content());
            if (data instanceof LastHttpContent) {
                // release queue defensively (event serialization and the done flag 
                // should mean there are no more items on the queue)
                releaseQueue();
                channelSubscription.cancel();
                subscriber.onComplete();
                return true;
            } else {
                return false;
            }
        }

        // Should only be called from the drain loop. We want to poll
        // the whole queue and release the contents one by one so we
        // need to honor the single consumer aspect of the Spsc queue
        // to ensure proper visibility of the queued items.
        private void releaseQueue() {
            HttpContent c;
            while ((c = queue.poll()) != null) {
                c.release();
            }
        }

    }

    private static final class HttpClientInboundHandler extends ChannelInboundHandlerAdapter {
        private SingleEmitter<HttpResponse> responseEmitter;
        private ResponseContentFlowable contentEmitter;
        private Subscription requestContentSubscription;
        private final NettyAdapter adapter;
        private boolean didEmitHttpResponse;

        private HttpClientInboundHandler(NettyAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            adapter.channelPool.release(ctx.channel());
            if (contentEmitter != null) {
                contentEmitter.onError(cause);
            } else if (responseEmitter != null && !responseEmitter.isDisposed()) {
                responseEmitter.onError(cause);
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            if (contentEmitter != null) {
                contentEmitter.chunkCompleted();
            }
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            if (ctx.channel().isWritable()) {
                requestContentSubscription.request(1);
            }

            super.channelWritabilityChanged(ctx);
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof io.netty.handler.codec.http.HttpResponse) {
                io.netty.handler.codec.http.HttpResponse response = (io.netty.handler.codec.http.HttpResponse) msg;

                if (response.decoderResult().isFailure()) {
                    exceptionCaught(ctx, response.decoderResult().cause());
                    return;
                }

                contentEmitter = new ResponseContentFlowable(new Subscription() {
                    @Override
                    public void request(long n) {
                        Preconditions.checkArgument(n == 1, "requests must be one at a time!");
                        ctx.channel().read();
                    }

                    @Override
                    public void cancel() {
                        ctx.channel().eventLoop().execute(() -> {
                            if (contentEmitter != null) {
                                adapter.channelPool.closeAndRelease(ctx.channel());
                                contentEmitter = null;
                            }
                        });
                    }
                });

                // Prevents channel from being closed when the Single<HttpResponse> is disposed
                didEmitHttpResponse = true;

                //Scheduler scheduler = Schedulers.from(ctx.channel().eventLoop());
                responseEmitter.onSuccess(
                        new NettyResponse(response, contentEmitter));
            }

            if (msg instanceof HttpContent) {
                HttpContent content = (HttpContent) msg;

                // channelRead can still come through even after a Subscription.cancel event
                if (contentEmitter != null) {
                    contentEmitter.onReceivedContent(content);
                }
            }

            if (msg instanceof LastHttpContent) {
                contentEmitter = null;
                adapter.channelPool.release(ctx.channel());
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (contentEmitter!=null) {
                contentEmitter.channelInactive();
            }
            super.channelInactive(ctx);
        }
        
        
    }

    /**
     * The factory for creating a NettyClient.
     */
    public static class Factory implements HttpClientFactory {
        private final NettyAdapter adapter;

        /**
         * Create a Netty client factory with default settings.
         */
        public Factory() {
            this.adapter = new NettyAdapter();
        }

        /**
         * Create a Netty client factory, specifying the event loop group size and the
         * channel pool size.
         * 
         * @param eventLoopGroupSize
         *            the number of event loop executors
         * @param channelPoolSize
         *            the number of pooled channels (connections)
         */
        public Factory(int eventLoopGroupSize, int channelPoolSize) {
            this.adapter = new NettyAdapter(eventLoopGroupSize, channelPoolSize);
        }

        @Override
        public HttpClient create(final HttpClientConfiguration configuration) {
            return new NettyClient(configuration, adapter);
        }

        @Override
        public void close() {
            adapter.shutdownGracefully().awaitUninterruptibly();
        }
    }
}
