/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A HTTP request body that contains a chunk of a file.
 */
public class SharedChannelPool implements ChannelPool {
    private final Bootstrap bootstrap;
    private final ChannelPoolHandler handler;
    private boolean closed = false;
    private final int poolSize;
    private final Queue<ChannelRequest> requests;
    private final Queue<Channel> available;
    private final Queue<Channel> leased;
    private final Object sync = -1;

    public SharedChannelPool(final Bootstrap bootstrap, final ChannelPoolHandler handler, int size) {
        this.bootstrap = bootstrap.clone().handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                assert ch.eventLoop().inEventLoop();
                handler.channelCreated(ch);
            }
        });
        this.handler = handler;
        this.poolSize = size;
        this.requests = new ConcurrentLinkedDeque<>();
        this.available = new ConcurrentLinkedDeque<>();
        this.leased = new ConcurrentLinkedDeque<>();
        bootstrap.config().group().submit(new Runnable() {
            @Override
            public void run() {
                while (!closed) {
                    try {
                        final ChannelRequest request;
                        final ChannelFuture channelFuture;
                        if (requests.isEmpty() && !closed) {
                            synchronized (requests) {
                                requests.wait();
                            }
                        }
                        request = requests.poll();

                        synchronized (sync) {
                            if (leased.size() >= poolSize && !closed) {
                                sync.wait();
                            }
                            if (!available.isEmpty()) {
                                Channel channel = available.poll();
                                channelFuture = channel.connect(new InetSocketAddress(request.host, request.port));
                            } else {
                                channelFuture = SharedChannelPool.this.bootstrap.clone().connect(request.host, request.port);
                            }
                            channelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                                @Override
                                public void operationComplete(Future<? super Void> future) throws Exception {
                                    if (future.isSuccess()) {
                                        handler.channelAcquired(channelFuture.channel());
                                        request.promise.setSuccess(channelFuture.channel());
                                    } else {
                                        request.promise.setFailure(future.cause());
                                        throw new RuntimeException(future.cause());
                                    }
                                }
                            });
                            leased.add(channelFuture.channel());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    public Future<Channel> acquire(String host, int port) {
        return this.acquire(host, port, this.bootstrap.config().group().next().<Channel>newPromise());
    }

    public Future<Channel> acquire(String host, int port, final Promise<Channel> promise) {
        ChannelRequest channelRequest = new ChannelRequest();
        channelRequest.promise = promise;
        channelRequest.host = host;
        channelRequest.port = port;
        requests.add(channelRequest);
        synchronized (requests) {
            requests.notify();
        }
        return channelRequest.promise;
    }

    @Override
    public Future<Channel> acquire() {
        throw new UnsupportedOperationException("Please pass host & port to shared channel pool.");
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        throw new UnsupportedOperationException("Please pass host & port to shared channel pool.");
    }

    @Override
    public Future<Void> release(final Channel channel) {
        return this.release(channel, this.bootstrap.config().group().next().<Void>newPromise());
    }

    @Override
    public Future<Void> release(final Channel channel, final Promise<Void> promise) {
        return channel.disconnect().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                if (future.isSuccess()) {
                    handler.channelReleased(channel);
                    promise.setSuccess((Void) future.getNow());
                    synchronized (sync) {
                        leased.remove(channel);
                        available.add(channel);
                        sync.notify();
                    }
                } else {
                    promise.setFailure(future.cause());
                    throw new RuntimeException(future.cause());
                }
            }
        });
    }

    @Override
    public void close() {
        closed = true;
        synchronized (sync) {
            for (Channel channel : leased) {
                channel.close();
            }
            for (Channel channel : available) {
                channel.close();
            }
        }
    }

    private static class ChannelRequest {
        private String host;
        private int port;
        private Promise<Channel> promise;
    }
}
