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
    private final ConcurrentMultiHashMap<InetSocketAddress, Channel> available;
    private final ConcurrentMultiHashMap<InetSocketAddress, Channel> leased;
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
        this.available = new ConcurrentMultiHashMap<>();
        this.leased = new ConcurrentMultiHashMap<>();
        bootstrap.config().group().submit(new Runnable() {
            @Override
            public void run() {
                while (!closed) {
                    try {
                        final ChannelRequest request;
                        final ChannelFuture channelFuture;
                        synchronized (requests) {
                            if (requests.isEmpty() && !closed) {
                                requests.wait();
                            }
                        }
                        request = requests.poll();

                        if (leased.size() >= poolSize && !closed) {
                            synchronized (sync) {
                                sync.wait();
                            }
                        }
                        if (closed) {
                            break;
                        }
                        synchronized (sync) {
                            InetSocketAddress address = new InetSocketAddress(request.host, request.port);
                            if (available.containsKey(address)) {
                                Channel channel = available.poll(address);
                                channelFuture = channel.newSucceededFuture();
                                handler.channelAcquired(channelFuture.channel());
                                request.promise.setSuccess(channelFuture.channel());
                            } else {
                                if (available.size() > 0 && available.size() + leased.size() >= poolSize) {
                                    available.poll().closeFuture();
                                }
                                channelFuture = SharedChannelPool.this.bootstrap.clone().connect(address);
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
                            }
                            leased.put(address, channelFuture.channel());
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
        try {
            handler.channelReleased(channel);
        } catch (Exception e) {
            promise.setFailure(e);
            return promise;
        }
        promise.setSuccess(null);
        synchronized (sync) {
            leased.remove((InetSocketAddress) channel.remoteAddress(), channel);
            available.put((InetSocketAddress) channel.remoteAddress(), channel);
            sync.notify();
        }
        return promise;
    }

    @Override
    public void close() {
        closed = true;
        synchronized (sync) {
            for (Channel channel : leased.values()) {
                channel.close();
            }
            for (Channel channel : available.values()) {
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
