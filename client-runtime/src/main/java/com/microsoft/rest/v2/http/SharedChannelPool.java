/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.FailedFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.SucceededFuture;
import io.reactivex.annotations.Nullable;
import io.reactivex.exceptions.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Netty channel pool implementation shared between multiple requests.
 *
 * Requests with the same host, port, and scheme share the same internal
 * pool. All the internal pools for all the requests have a fixed size limit.
 * This channel pool should be shared between multiple Netty adapters.
 */
class SharedChannelPool implements ChannelPool {
    private static final AttributeKey<URI> CHANNEL_URI = AttributeKey.newInstance("channel-uri");
    private static final AttributeKey<ZonedDateTime> CHANNEL_AVAILABLE_SINCE = AttributeKey.newInstance("channel-available-since");
    private static final AttributeKey<ZonedDateTime> CHANNEL_LEASED_SINCE = AttributeKey.newInstance("channel-leased-since");
    private static final AttributeKey<ZonedDateTime> CHANNEL_CREATED_SINCE = AttributeKey.newInstance("channel-created-since");
    private static final AttributeKey<ZonedDateTime> CHANNEL_CLOSED_SINCE = AttributeKey.newInstance("channel-closed-since");
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final ChannelPoolHandler handler;
    private final int poolSize;
    private final AtomicInteger channelCount = new AtomicInteger(0);
    private final SharedChannelPoolOptions poolOptions;
    private final ConcurrentMultiDequeMap<URI, ChannelRequest> requests;
    private final ConcurrentMultiDequeMap<URI, Channel> available;
    private final ConcurrentMultiDequeMap<URI, Channel> leased;
    private final Object sync = new Object();
    private final SslContext sslContext;
    private volatile boolean closed = false;
    private final Logger logger = LoggerFactory.getLogger(SharedChannelPool.class);
    AtomicInteger wip = new AtomicInteger(0);

    private boolean isChannelHealthy(Channel channel) {
        try {
            if (!channel.isActive()) {
                return false;
            } else if (channel.pipeline().get("HttpResponseDecoder") == null && channel.pipeline().get("HttpClientCodec") == null) {
                return false;
            } else {
                ZonedDateTime channelAvailableSince = channel.attr(CHANNEL_AVAILABLE_SINCE).get();
                if (channelAvailableSince == null) {
                    channelAvailableSince = channel.attr(CHANNEL_LEASED_SINCE).get();
                }
                final long channelIdleDurationInSec = ChronoUnit.SECONDS.between(channelAvailableSince, ZonedDateTime.now(ZoneOffset.UTC));
                return channelIdleDurationInSec < this.poolOptions.idleChannelKeepAliveDurationInSec();
            }
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Creates an instance of the shared channel pool.
     * @param bootstrap the bootstrap to create channels
     * @param handler the handler to apply to the channels on creation, acquisition and release
     * @param options optional settings for the pool
     * @param sslContext the SSL Context for the connections
     */
    SharedChannelPool(final Bootstrap bootstrap, final EventLoopGroup eventLoopGroup, final ChannelPoolHandler handler, SharedChannelPoolOptions options, SslContext sslContext) {
        this.poolOptions = options.clone();
        this.bootstrap = bootstrap.clone();
        this.eventLoopGroup = eventLoopGroup;
        this.handler = handler;
        this.poolSize = options.poolSize();
        this.requests = new ConcurrentMultiDequeMap<>();
        this.available = new ConcurrentMultiDequeMap<>();
        this.leased = new ConcurrentMultiDequeMap<>();
        try {
            if (sslContext == null) {
                this.sslContext = SslContextBuilder.forClient().build();
            } else {
                this.sslContext = sslContext;
            }
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private void drain(URI preferredUri) {
        if (!wip.compareAndSet(0, 1)) {
            return;
        }
        while (!closed && wip.updateAndGet(x -> requests.size()) != 0) {
            if (channelCount.get() >= poolSize && available.size() == 0) {
                wip.set(0);
                break;
            }
            // requests must be non-empty based on the above condition
            ChannelRequest request;
            if (preferredUri != null && requests.containsKey(preferredUri)) {
                request = requests.poll(preferredUri);
            } else {
                request = requests.poll();
            }

            boolean foundHealthyChannelInPool = false;
            // Try to retrieve a healthy channel from pool
            if (available.containsKey(request.channelURI)) {
                Channel channel = available.pop(request.channelURI); // try most recently used
                if (isChannelHealthy(channel)) {
                    logger.debug("Channel picked up from pool: {}", channel.id());
                    leased.put(request.channelURI, channel);
                    foundHealthyChannelInPool = true;
                    channel.attr(CHANNEL_LEASED_SINCE).set(ZonedDateTime.now(ZoneOffset.UTC));
                    request.promise.setSuccess(channel);
                    try {
                        handler.channelAcquired(channel);
                    } catch (Exception e) {
                        throw Exceptions.propagate(e);
                    }
                } else {
                    logger.debug("Channel disposed from pool due to timeout or half closure: {}", channel.id());
                    closeChannel(channel);
                    channelCount.decrementAndGet();
                    // Delete all channels created before this
                    while (available.containsKey(request.channelURI)) {
                        Channel broken = available.pop(request.channelURI);
                        logger.debug("Channel disposed from pool due to timeout or half closure: {}", broken.id());
                        closeChannel(broken);
                        channelCount.decrementAndGet();
                    }
                }
            }
            if (!foundHealthyChannelInPool) {
                // Not found a healthy channel in pool. Create a new channel - remove an available one if size overflows
                if (channelCount.get() >= poolSize) {
                    Channel nextAvailable = available.poll(); // Dispose least recently used
                    logger.debug("Channel disposed due to overflow: {}", nextAvailable.id());
                    closeChannel(nextAvailable);
                    channelCount.decrementAndGet();
                }
                int port;
                if (request.destinationURI.getPort() < 0) {
                    port = "https".equals(request.destinationURI.getScheme()) ? 443 : 80;
                } else {
                    port = request.destinationURI.getPort();
                }
                channelCount.incrementAndGet();
                SharedChannelPool.this.bootstrap.clone().handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        assert ch.eventLoop().inEventLoop();
                        if (request.proxy != null) {
                            ch.pipeline().addFirst("HttpProxyHandler", new HttpProxyHandler(request.proxy.address()));
                        }
                        handler.channelCreated(ch);
                    }
                }).connect(request.destinationURI.getHost(), port).addListener((ChannelFuture f) -> {
                    if (f.isSuccess()) {
                        Channel channel = f.channel();
                        channel.attr(CHANNEL_URI).set(request.channelURI);

                        // Apply SSL handler for https connections
                        if ("https".equalsIgnoreCase(request.destinationURI.getScheme())) {
                            channel.pipeline().addBefore("HttpClientCodec", "SslHandler", this.sslContext.newHandler(channel.alloc(), request.destinationURI.getHost(), port));
                        }
                        leased.put(request.channelURI, channel);
                        channel.attr(CHANNEL_CREATED_SINCE).set(ZonedDateTime.now(ZoneOffset.UTC));
                        channel.attr(CHANNEL_LEASED_SINCE).set(ZonedDateTime.now(ZoneOffset.UTC));
                        logger.debug("Channel created: {}", channel.id());
                        handler.channelAcquired(channel);
                        request.promise.setSuccess(channel);
                    } else {
                        request.promise.setFailure(f.cause());
                        channelCount.decrementAndGet();
                    }
                });
            }
        }
    }

    /**
     * Acquire a channel for a URI.
     * @param uri the URI the channel acquired should be connected to
     * @return the future to a connected channel
     */
    public Future<Channel> acquire(URI uri, @Nullable Proxy proxy) {
        return this.acquire(uri, proxy, this.bootstrap.config().group().next().<Channel>newPromise());
    }

    /**
     * Acquire a channel for a URI.
     * @param uri the URI the channel acquired should be connected to
     * @param promise the writable future to a connected channel
     * @return the future to a connected channel
     */
    public Future<Channel> acquire(URI uri, @Nullable Proxy proxy, final Promise<Channel> promise) {
        if (closed) {
            throw new RejectedExecutionException("SharedChannelPool is closed");
        }

        ChannelRequest channelRequest = new ChannelRequest();
        channelRequest.promise = promise;
        channelRequest.proxy = proxy;
        int port;
        if (uri.getPort() < 0) {
            port = "https".equals(uri.getScheme()) ? 443 : 80;
        } else {
            port = uri.getPort();
        }
        try {
            channelRequest.destinationURI = new URI(String.format("%s://%s:%d", uri.getScheme(), uri.getHost(), port));

            if (proxy == null) {
                channelRequest.channelURI = channelRequest.destinationURI;
            } else {
                InetSocketAddress address = (InetSocketAddress) proxy.address();
                channelRequest.channelURI = new URI(String.format("%s://%s:%d", uri.getScheme(), address.getHostString(), address.getPort()));
            }

            requests.put(channelRequest.channelURI, channelRequest);
            drain(null);
        } catch (URISyntaxException e) {
            promise.setFailure(e);
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

    private Future<Void> closeChannel(final Channel channel) {
        if (!channel.isOpen()) {
            return new SucceededFuture<>(eventLoopGroup.next(), null);
        }
        channel.attr(CHANNEL_CLOSED_SINCE).set(ZonedDateTime.now(ZoneOffset.UTC));
        logger.debug("Channel initiated to close: " + channel.id());
        // Closing a channel doesn't change the channel count
        try {
            return channel.close().addListener(f -> {
                if (!f.isSuccess()) {
                    logger.warn("Possible channel leak: failed to close " + channel.id(), f.cause());
                }
            });
        } catch (Exception e) {
            logger.warn("Possible channel leak: failed to close " + channel.id(), e);
            return new FailedFuture<>(eventLoopGroup.next(), e);
        }
    }

    /**
     * Closes the channel and releases it back to the pool.
     * @param channel the channel to close and release.
     * @return a Future representing the operation.
     */
    public Future<Void> closeAndRelease(final Channel channel) {
        try {
            Future<Void> closeFuture = closeChannel(channel).addListener(future -> {
                URI channelUri = channel.attr(CHANNEL_URI).get();
                if (leased.remove(channelUri, channel) || available.remove(channelUri, channel)) {
                    channelCount.decrementAndGet();
                    logger.debug("Channel closed and released out of pool: " + channel.id());
                }
                drain(channelUri);
            });
            return closeFuture;
        } catch (Exception e) {
            return bootstrap.config().group().next().newFailedFuture(e);
        }
    }

    @Override
    public Future<Void> release(final Channel channel) {
        try {
            handler.channelReleased(channel);
            URI channelUri = channel.attr(CHANNEL_URI).get();
            leased.remove(channelUri, channel);
            if (isChannelHealthy(channel)) {
                available.put(channelUri, channel);
                channel.attr(CHANNEL_AVAILABLE_SINCE).set(ZonedDateTime.now(ZoneOffset.UTC));
                logger.debug("Channel released to pool: " + channel.id());
            } else {
                channelCount.decrementAndGet();
                logger.debug("Channel broken on release, dispose: " + channel.id());
            }
            drain(channelUri);
        } catch (Exception e) {
            return bootstrap.config().group().next().newFailedFuture(e);
        }
        return bootstrap.config().group().next().newSucceededFuture(null);
    }

    @Override
    public Future<Void> release(final Channel channel, final Promise<Void> promise) {
        return release(channel).addListener(f -> {
            if (f.isSuccess()) {
                promise.setSuccess(null);
            } else {
                promise.setFailure(f.cause());
            }
        });
    }

    @Override
    public void close() {
        closed = true;
        while (requests.size() == 0) {
            requests.poll().promise.setFailure(new CancellationException("Channel pool was closed"));
        }
    }

    private static class ChannelRequest {
        private URI destinationURI;
        private URI channelURI;
        private Proxy proxy;
        private Promise<Channel> promise;
    }

    /**
     * Used to print a current overview of the channels in this pool.
     */
    public void dump() {
        logger.info(String.format("---- %s: size %d, keep alive (sec) %d ----", toString(), poolSize, poolOptions.idleChannelKeepAliveDurationInSec()));
        logger.info("Channel\tState\tFor\tAge\tURL");
        List<Channel> closed = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        for (Channel channel : leased.values()) {
            if (channel.hasAttr(CHANNEL_CLOSED_SINCE)) {
                closed.add(channel);
                continue;
            }
            long stateFor = ChronoUnit.SECONDS.between(channel.attr(CHANNEL_LEASED_SINCE).get(), now);
            long age = ChronoUnit.SECONDS.between(channel.attr(CHANNEL_CREATED_SINCE).get(), now);
            logger.info(String.format("%s\tLEASE\t%ds\t%ds\t%s", channel.id(), stateFor, age, channel.attr(CHANNEL_URI).get()));
        }
        for (Channel channel : available.values()) {
            if (channel.hasAttr(CHANNEL_CLOSED_SINCE)) {
                closed.add(channel);
                continue;
            }
            long stateFor = ChronoUnit.SECONDS.between(channel.attr(CHANNEL_AVAILABLE_SINCE).get(), now);
            long age = ChronoUnit.SECONDS.between(channel.attr(CHANNEL_CREATED_SINCE).get(), now);
            logger.info(String.format("%s\tAVAIL\t%ds\t%ds\t%s", channel.id(), stateFor, age, channel.attr(CHANNEL_URI).get()));
        }
        for (Channel channel : closed) {
            long stateFor = ChronoUnit.SECONDS.between(channel.attr(CHANNEL_CLOSED_SINCE).get(), now);
            long age = ChronoUnit.SECONDS.between(channel.attr(CHANNEL_CREATED_SINCE).get(), now);
            logger.info(String.format("%s\tCLOSE\t%ds\t%ds\t%s", channel.id(), stateFor, age, channel.attr(CHANNEL_URI).get()));
        }
        logger.info("Active channels: " + channelCount.get() + " Leaked or being initialized channels: " + (channelCount.get() - leased.size() - available.size()));
    }
}
