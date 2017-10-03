/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.http;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.util.concurrent.CompleteFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.SucceededFuture;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A HTTP request body that contains a chunk of a file.
 */
public class SharedChannelPool extends SimpleChannelPool {
    private boolean closed = false;
    private final int poolSize;
    private Queue<SettableFuture<Channel>> requests;
    private final AtomicInteger poolAvailable;

    public SharedChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler, int size) {
        super(bootstrap, handler);
        this.poolSize = size;
        this.poolAvailable = new AtomicInteger(size);
        this.requests = new ConcurrentLinkedDeque<>();
        bootstrap.config().group().submit(new Runnable() {
            @Override
            public void run() {
                while (!closed) {
                    try {
                        while (requests.isEmpty()) {
                            Thread.sleep(100);
                        }

                        while (poolAvailable.get() <= 0 && !closed) {
                            synchronized (poolAvailable) {
                                poolAvailable.wait(1000);
                            }
                        }
                        if (!closed) {
                            Future<Channel> cf = SharedChannelPool.super.acquire();
                            if (cf.isSuccess()) {
                                requests.poll().set(cf.getNow());
                            } else {
                                requests.poll().setException(cf.cause());
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
    }

    @Override
    public Future<Channel> acquire(final Promise<Channel> promise) {
        SettableFuture<Channel> res = SettableFuture.create();
        SettableFuture<Channel> future = SettableFuture.create();
        requests.add(future);
        Futures.transformAsync(future, new AsyncFunction<Channel, Channel>() {
            @Override
            public ListenableFuture<Channel> apply(Channel input) throws Exception {
                return null;
            }
        })
    }

    @Override
    public void close() {
        closed = true;
        super.close();
    }

    @Override
    protected ChannelFuture connectChannel(Bootstrap bs) {
        return new DefaultChannelPromise()
    }
}
