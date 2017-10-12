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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import rx.Emitter;
import rx.Emitter.BackpressureMode;
import rx.Observable;
import rx.Single;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.subjects.ReplaySubject;

import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An HttpClient that is implemented using Netty.
 */
public class NettyAdapter extends HttpClient {
    private static final String HEADER_CONTENT_LENGTH = "Content-Length";
    private final NioEventLoopGroup eventLoopGroup;
    private final SharedChannelPool pool;

    /**
     * Creates NettyClient.
     * @param policyFactories the sequence of RequestPolicies to apply when sending HTTP requests.
     * @param handlerConfigs the Netty ChannelHandler configurations.
     */
    public NettyAdapter(List<RequestPolicy.Factory> policyFactories, final List<ChannelHandlerConfig> handlerConfigs) {
        super(policyFactories);
        this.eventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.MINUTES.toMillis(3L));
        pool = new SharedChannelPool(bootstrap, new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel ch) throws Exception {
                ch.pipeline().addLast("HttpResponseDecoder", new HttpResponseDecoder());
                ch.pipeline().addLast("HttpRequestEncoder", new HttpRequestEncoder());
                ch.pipeline().addLast("HttpClientInboundHandler", new HttpClientInboundHandler(NettyAdapter.this));

                if (handlerConfigs != null) {
                    for (ChannelHandlerConfig config : handlerConfigs) {
                        if (config.mayBlock()) {
                            ch.pipeline().addLast(eventLoopGroup, config.factory().call());
                        } else {
                            ch.pipeline().addLast(config.factory().call());
                        }
                    }
                }
            }
        }, this.eventLoopGroup.executorCount() * 2);
    }

    @Override
    public Single<HttpResponse> sendRequestInternalAsync(final HttpRequest request) {
        final URI uri;
        try {
            uri = new URI(request.url());
            request.withHeader(io.netty.handler.codec.http.HttpHeaders.Names.HOST, uri.getHost());
            request.withHeader(io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION, io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE);
        } catch (URISyntaxException e) {
            return Single.error(e);
        }

        // Creates cold observable from an emitter
        return Observable.fromEmitter(new Action1<Emitter<HttpResponse>>() {
            @Override
            public void call(final Emitter<HttpResponse> emitter) {
                pool.acquire(uri).addListener(new GenericFutureListener<Future<? super Channel>>() {
                    @Override
                    public void operationComplete(Future<? super Channel> cf) throws Exception {
                        if (!cf.isSuccess()) {
                            emitter.onError(cf.cause());
                            return;
                        }

                        final Channel channel = (Channel) cf.getNow();

                        HttpClientInboundHandler inboundHandler = channel.pipeline().get(HttpClientInboundHandler.class);
                        if (request.httpMethod().equalsIgnoreCase("HEAD")) {
                            // Use HttpClientCodec for HEAD operations
                            if (channel.pipeline().get("HttpClientCodec") == null) {
                                channel.pipeline().remove(HttpRequestEncoder.class);
                                channel.pipeline().replace(HttpResponseDecoder.class, "HttpClientCodec", new HttpClientCodec());
                            }
                            inboundHandler.contentExpected = false;
                        } else {
                            // Use HttpResponseDecoder for other operations
                            if (channel.pipeline().get("HttpResponseDecoder") == null) {
                                channel.pipeline().replace(HttpClientCodec.class, "HttpResponseDecoder", new HttpResponseDecoder());
                                channel.pipeline().addAfter("HttpResponseDecoder", "HttpRequestEncoder", new HttpRequestEncoder());
                            }
                            inboundHandler.contentExpected = true;
                        }
                        inboundHandler.responseEmitter = emitter;

                        final DefaultFullHttpRequest raw;
                        if (request.body() == null || request.body().contentLength() == 0) {
                            raw = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                                    HttpMethod.valueOf(request.httpMethod()),
                                    request.url());
                        } else {
                            ByteBuf requestContent;
                            if (request.body() instanceof ByteArrayHttpRequestBody) {
                                requestContent = Unpooled.wrappedBuffer(((ByteArrayHttpRequestBody) request.body()).content());
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
                        for (HttpHeader header : request.headers()) {
                            raw.headers().add(header.name(), header.value());
                        }
                        raw.headers().add(HEADER_CONTENT_LENGTH, raw.content().readableBytes());
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
        }, BackpressureMode.BUFFER).toSingle();
    }

    private static final class HttpClientInboundHandler extends ChannelInboundHandlerAdapter {

        private ReplaySubject<ByteBuf> contentEmitter;
        private Emitter<HttpResponse> responseEmitter;
        private NettyAdapter adapter;
        private long contentLength;
        private boolean contentExpected;

        private HttpClientInboundHandler(NettyAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            adapter.pool.release(ctx.channel());
            responseEmitter.onError(cause);
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof io.netty.handler.codec.http.HttpResponse) {
                io.netty.handler.codec.http.HttpResponse response = (io.netty.handler.codec.http.HttpResponse) msg;

                if (response.decoderResult().isFailure()) {
                    exceptionCaught(ctx, response.decoderResult().cause());
                    return;
                }

                if (response.headers().contains(HEADER_CONTENT_LENGTH)) {
                    contentLength = Long.parseLong(response.headers().get(HEADER_CONTENT_LENGTH));
                }

                contentEmitter = ReplaySubject.create();
                responseEmitter.onNext(new NettyResponse(response, contentEmitter));
            }
            if (msg instanceof HttpContent) {
                HttpContent content = (HttpContent) msg;
                ByteBuf buf = content.content();

                if (contentLength == 0 || !contentExpected) {
                    contentEmitter.onNext(buf);
                    contentEmitter.onCompleted();
                    responseEmitter.onCompleted();
                    adapter.pool.release(ctx.channel());
                    return;
                }

                if (contentLength > 0 && buf != null && buf.readableBytes() > 0) {
                    int readable = buf.readableBytes();
                    contentLength -= readable;
                    contentEmitter.onNext(buf);
                }

                if (contentLength == 0) {
                    contentEmitter.onCompleted();
                    responseEmitter.onCompleted();
                    adapter.pool.release(ctx.channel());
                }
            }
        }
    }

    /**
     * The factory for creating a NettyAdapter.
     */
    public static class Factory implements HttpClient.Factory {
        @Override
        public HttpClient create(final Configuration configuration) {
            final List<ChannelHandlerConfig> channelHandlerConfigs;
            final Proxy proxy = configuration.proxy();
            if (proxy != null) {
                ChannelHandlerConfig channelHandlerConfig = new ChannelHandlerConfig(new Func0<ChannelHandler>() {
                    @Override
                    public ChannelHandler call() {
                        return new HttpProxyHandler(proxy.address());
                    }
                }, false);
                channelHandlerConfigs = Collections.singletonList(channelHandlerConfig);
            } else {
                channelHandlerConfigs = Collections.emptyList();
            }

            return new NettyAdapter(configuration.policyFactories(), channelHandlerConfigs);
        }
    }
}
