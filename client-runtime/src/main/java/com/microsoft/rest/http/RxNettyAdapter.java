/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.http;

import com.microsoft.rest.policy.RequestPolicy;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Future;
import rx.Observable;
import rx.Observer;
import rx.Single;
import rx.observables.SyncOnSubscribe;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
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
    private final ChannelPool pool;
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
        pool = new FixedChannelPool(bootstrap.remoteAddress(host, port), new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel ch) throws Exception {
                if (sslContext != null) {
                    ch.pipeline().addLast(sslContext.newHandler(ch.alloc(), host, port));
                }
                ch.pipeline().addLast(new HttpResponseDecoder());
                ch.pipeline().addLast(new HttpRequestEncoder());
                ch.pipeline().addLast(new RetryChannelHandler(NettyRxAdapter.this));
                ch.pipeline().addLast(new HttpClientInboundHandler(NettyRxAdapter.this));
//                ch.pipeline().addFirst(new HttpProxyHandler(new InetSocketAddress("localhost", 8888)));
            }

            @Override
            public void channelReleased(Channel ch) throws Exception {
                ch.attr(RETRY_COUNT).set(0);
            }
        }, this.channelPoolSize);
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
    public Single<HttpResponse> sendRequestInternalAsync(HttpRequest request) {
        return Observable.defer(() -> {
            URI uri = null;
            try {
                uri = new URI(request.url());
                request.withHeader(io.netty.handler.codec.http.HttpHeaders.Names.HOST, uri.getHost());
                request.withHeader(io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION, io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE);
            } catch (URISyntaxException e) {
                return Observable.error(e);
            }

            Future<Channel> future = pool.acquire();

            return Observable.<ByteBuf>fromEmitter(emitter -> future.addListener(cf -> {
                if (!cf.isSuccess()) {
                    emitter.onError(cf.cause());
                    return;
                }

                Channel channel = (Channel) cf.getNow();

                channel.attr(REQUEST_PROVIDER).set(request);

                if (channel.pipeline().last() == null) {
                    channel.pipeline().addLast(new HttpResponseDecoder());
                    channel.pipeline().addLast(new HttpRequestEncoder());
                    channel.pipeline().addLast(new RetryChannelHandler(NettyRxAdapter.this));
                    channel.pipeline().addLast(new HttpClientInboundHandler(NettyRxAdapter.this));
                }

                channel.pipeline().get(HttpClientInboundHandler.class).setEmitter(emitter);


                FullHttpRequest raw = request.provide();
                String range = raw.headers().get("x-ms-range");
                channel.attr(RANGE).set(range);
                channel.writeAndFlush(raw).addListener(v -> {
                    if (v.isSuccess()) {
                        channel.read();
                    } else {
                        emitter.onError(v.cause());
                    }
                });
            }), BackpressureMode.BUFFER)
                    .retryWhen(observable -> observable.zipWith(Observable.range(1, 10), (throwable, integer) -> integer)
                            .flatMap(i -> Observable.timer(i, TimeUnit.SECONDS)))
                    .toList()
                    .map(l -> {
                        ByteBuf[] bufs = new ByteBuf[l.size()];
                        return Unpooled.wrappedBuffer(l.toArray(bufs));
                    });
        });
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
