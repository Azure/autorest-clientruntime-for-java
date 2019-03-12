/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.http;

/**
 * Optional configurations for http channel pool.
 */
public class SharedChannelPoolOptions {
    // Default duration in sec to keep the connection alive in available pool before closing it.
    private static final long DEFAULT_TTL_OF_IDLE_CHANNEL = 60;
    private long idleChannelKeepAliveDurationInSec;
    private int poolSize;

    /**
     * Creates SharedChannelPoolOptions.
     */
    public SharedChannelPoolOptions() {
        this.idleChannelKeepAliveDurationInSec = DEFAULT_TTL_OF_IDLE_CHANNEL;
    }

    /**
     * Duration in sec to keep the connection alive in available pool before closing it.
     *
     * @param ttlDurationInSec the duration
     * @return SharedChannelPoolOptions
     */
    public SharedChannelPoolOptions withIdleChannelKeepAliveDurationInSec(long ttlDurationInSec) {
        this.idleChannelKeepAliveDurationInSec = ttlDurationInSec;
        return this;
    }

    /**
     * @return gets duration in sec the connection alive in available pool before closing it.
     */
    public long idleChannelKeepAliveDurationInSec() {
        return this.idleChannelKeepAliveDurationInSec;
    }

    /**
     * Sets the max number of connections allowed in the pool.
     * @param poolSize the size of the pool
     * @return SharedChannelPoolOptions
     */
    public SharedChannelPoolOptions withPoolSize(int poolSize) {
        this.poolSize = poolSize;
        return this;
    }

    /**
     * @return the max number of connections allowed in the pool
     */
    public int poolSize() {
        return poolSize;
    }

    @Override
    public SharedChannelPoolOptions clone() {
        return new SharedChannelPoolOptions()
                .withIdleChannelKeepAliveDurationInSec(this.idleChannelKeepAliveDurationInSec);
    }
}
