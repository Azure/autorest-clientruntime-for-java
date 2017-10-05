/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.http;

import com.microsoft.rest.policy.RequestPolicy;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import rx.Emitter;
import rx.Emitter.BackpressureMode;
import rx.Observable;
import rx.Observer;
import rx.Single;
import rx.SingleEmitter;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.observables.SyncOnSubscribe;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A HttpClient that is implemented using RxNetty.
 */
public class RxNettyAdapter extends HttpClient {
    private final List<ChannelHandlerConfig> handlerConfigs;
    private final NioEventLoopGroup eventLoopGroup;
    private final Bootstrap bootstrap;
    private SslContext sslContext;
    private final SharedChannelPool pool;
    private final static AttributeKey<Integer> RETRY_COUNT = AttributeKey.newInstance("retry-count");
    private final static AttributeKey<HttpRequest> REQUEST_PROVIDER = AttributeKey.newInstance("request-provider");

    /**
     * Creates RxNettyClient.
     * @param policyFactories the sequence of RequestPolicies to apply when sending HTTP requests.
     * @param handlerConfigs the Netty ChannelHandler configurations.
     */
    public RxNettyAdapter(List<RequestPolicy.Factory> policyFactories, List<ChannelHandlerConfig> handlerConfigs) {
        super(policyFactories);
        this.handlerConfigs = handlerConfigs;
        this.eventLoopGroup = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(eventLoopGroup);
        this.bootstrap.channel(NioSocketChannel.class);
        this.bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        this.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.MINUTES.toMillis(3L));
        try {
            sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } catch (SSLException e) {
            e.printStackTrace();
        }
        pool = new SharedChannelPool(bootstrap, new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel ch) throws Exception {
                ch.pipeline().addLast(new HttpResponseDecoder());
                ch.pipeline().addLast(new HttpRequestEncoder());
                ch.pipeline().addLast(new HttpClientInboundHandler(RxNettyAdapter.this));
            }

            @Override
            public void channelReleased(Channel ch) throws Exception {
                ch.attr(RETRY_COUNT).set(0);
            }
        }, this.eventLoopGroup.executorCount() * 2);
    }

    private SSLEngine getSSLEngine(String host) {
        SSLContext sslCtx;
        try {
            sslCtx = SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        SSLEngine engine = sslCtx.createSSLEngine(host, 443);
        engine.setUseClientMode(true);
        return engine;
    }

    @Override
    public Single<HttpResponse> sendRequestInternalAsync(final HttpRequest request) {
        return Single.defer(new Func0<Single<HttpResponse>>() {
            @Override
            public Single<HttpResponse> call() {
                final URI uri;
                try {
                    uri = new URI(request.url());
                    request.withHeader(io.netty.handler.codec.http.HttpHeaders.Names.HOST, uri.getHost());
                    request.withHeader(io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION, io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE);
                } catch (URISyntaxException e) {
                    return Single.error(e);
                }

                final String host = uri.getHost();
                final int port;
                if (uri.getPort() < 0) {
                    port = "https".equals(uri.getScheme()) ? 443 : 80;
                } else {
                    port = uri.getPort();
                }

                final Future<Channel> future = pool.acquire(host, port);

                return Single.fromEmitter(new Action1<SingleEmitter<HttpResponse>>() {
                    @Override
                    public void call(final SingleEmitter<HttpResponse> emitter) {
                        future.addListener(new GenericFutureListener<Future<? super Channel>>() {
                            @Override
                            public void operationComplete(Future<? super Channel> cf) throws Exception {
                                if (!cf.isSuccess()) {
                                    emitter.onError(cf.cause());
                                    return;
                                }

                                final Channel channel = (Channel) cf.getNow();

                                channel.attr(REQUEST_PROVIDER).set(request);

//                                if (channel.pipeline().get(SslHandler.class) == null) {
//                                    channel.pipeline().addFirst(sslContext.newHandler(channel.alloc(), host, port));
//                                }
                                if (channel.pipeline().last() == null) {
                                    channel.pipeline().addLast(new HttpResponseDecoder());
                                    channel.pipeline().addLast(new HttpRequestEncoder());
                                    channel.pipeline().addLast(new HttpClientInboundHandler(RxNettyAdapter.this));
                                }

                                channel.pipeline().get(HttpClientInboundHandler.class).setResponseEmitter(emitter);

                                final FullHttpRequest raw;
                                if (request.body() == null || request.body().contentLength() == 0) {
                                    raw = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                                            HttpMethod.valueOf(request.httpMethod()),
                                            request.url());
                                } else {
                                    ByteBuf requestContent;
                                    if (request.body() instanceof ByteArrayRequestBody) {
                                        requestContent = Unpooled.wrappedBuffer(((ByteArrayRequestBody) request.body()).content());
                                    } else if (request.body() instanceof FileRequestBody) {
                                        FileSegment segment = ((FileRequestBody) request.body()).content();
                                        requestContent = ByteBufAllocator.DEFAULT.buffer(segment.length());
                                        requestContent.writeBytes(segment.fileChannel(), segment.offset(), segment.length());
                                    } else {
                                        throw new IllegalArgumentException("Only ByteArrayRequestBody or FileRequestBody are supported");
                                    }
                                    raw = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                                            HttpMethod.valueOf(request.httpMethod()),
                                            request.url(),
                                            requestContent);
                                }
                                channel.writeAndFlush(raw).addListener(new GenericFutureListener<Future<? super Void>>() {
                                    @Override
                                    public void operationComplete(Future<? super Void> v) throws Exception {
                                        if (v.isSuccess()) {
                                            channel.read();
                                        } else {
                                            emitter.onError(v.cause());
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private static class HttpClientInboundHandler extends ChannelInboundHandlerAdapter {

        private static final String HEADER_CONTENT_LENGTH = "Content-Length";
        private Emitter<ByteBuf> contentEmitter;
        private SingleEmitter<HttpResponse> responseEmitter;
        private RxNettyAdapter adapter;
        private long contentLength;

        public HttpClientInboundHandler(RxNettyAdapter adapter) {
            this.adapter = adapter;
        }

        public void setResponseEmitter(SingleEmitter<HttpResponse> emitter) {
            this.responseEmitter = emitter;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            adapter.pool.release(ctx.channel());
            contentEmitter.onError(cause);
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof io.netty.handler.codec.http.HttpResponse)
            {
                io.netty.handler.codec.http.HttpResponse response = (io.netty.handler.codec.http.HttpResponse) msg;

                if (response.headers().contains(HEADER_CONTENT_LENGTH)) {
                    contentLength = Long.parseLong(response.headers().get(HEADER_CONTENT_LENGTH));
                }

                if (contentLength == 0) {
                    responseEmitter.onSuccess(new RxNettyResponse(response, Observable.just((ByteBuf) new EmptyByteBuf(ByteBufAllocator.DEFAULT))
                    .doOnCompleted(new Action0() {
                        @Override
                        public void call() {
                            adapter.pool.release(ctx.channel());
                        }
                    })));
                } else {
                    responseEmitter.onSuccess(new RxNettyResponse(response, Observable.fromEmitter(new Action1<Emitter<ByteBuf>>() {
                        @Override
                        public void call(Emitter<ByteBuf> byteBufEmitter) {
                            contentEmitter = byteBufEmitter;
                        }
                    }, BackpressureMode.BUFFER)));
                }
            }
            if(msg instanceof HttpContent)
            {
                if (contentLength == 0) {
                    return;
                }

                HttpContent content = (HttpContent)msg;
                ByteBuf buf = content.content();

                if (contentLength > 0 && buf != null && buf.readableBytes() > 0) {
                    int readable = buf.readableBytes();
                    contentLength -= readable;
                    contentEmitter.onNext(buf);
                }

                if (contentLength == 0) {
                    contentEmitter.onCompleted();
                    adapter.pool.release(ctx.channel());
                }
            }
        }
    }

    // This InputStream to Observable<byte[]> conversion comes from rxjava-string
    // (https://github.com/ReactiveX/RxJavaString). We can't just take a dependency on
    // rxjava-string, however, because they require an older version of rxjava (1.1.1).
    private static Observable<byte[]> toByteArrayObservable(InputStream inputStream) {
        return Observable.create(new OnSubscribeInputStream(inputStream, 8 * 1024));
    }

    private static final class OnSubscribeInputStream extends SyncOnSubscribe<InputStream, byte[]> {
        private final InputStream is;
        private final int size;

        OnSubscribeInputStream(InputStream is, int size) {
            this.is = is;
            this.size = size;
        }

        @Override
        protected InputStream generateState() {
            return this.is;
        }

        @Override
        protected InputStream next(InputStream state, Observer<? super byte[]> observer) {
            byte[] buffer = new byte[size];
            try {
                int count = state.read(buffer);
                if (count == -1) {
                    observer.onCompleted();
                } else if (count < size) {
                    observer.onNext(Arrays.copyOf(buffer, count));
                } else {
                    observer.onNext(buffer);
                }
            } catch (IOException e) {
                observer.onError(e);
            }
            return state;
        }
    }
}
