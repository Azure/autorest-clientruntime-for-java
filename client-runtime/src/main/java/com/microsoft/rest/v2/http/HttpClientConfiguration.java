/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.http;

import com.microsoft.rest.v2.annotations.Beta;

import java.net.Proxy;

/**
 * The set of parameters used to create an HTTP client.
 */
public class HttpClientConfiguration {
    private final Proxy proxy;
    private final String proxyScheme;
    private SharedChannelPoolOptions poolOptions;

    /**
     * @return The optional proxy to use.
     */
    public Proxy proxy() {
        return proxy;
    }

    /**
     * The scheme/protocol that will be used when sending the request to the proxy. If this is null, then the scheme
     * will be determined by the port of the proxy (80 will use 'http' and 443 will use 'https'). If the port of the
     * proxy isn't recognized (not 80 or 443), then the scheme of the final destination URI will be used.
     * @return
     */
    public String proxyScheme() {
        return proxyScheme;
    }

    /**
     * Creates an HttpClientConfiguration.
     * @param proxy The optional proxy to use.
     */
    public HttpClientConfiguration(Proxy proxy) {
        this(proxy, null);
    }

    /**
     * Creates an HttpClientConfiguration.
     * @param proxy The optional proxy to use.
     * @param proxyScheme The scheme/protocol that will be used when sending the request to the proxy. If this is null,
     *                    then the scheme will be determined by the port of the proxy (80 will use 'http' and 443 will
     *                    use 'https'). If the port of the proxy isn't 80 or 443, then the scheme of the final
     *                    destination URI will be used.
     */
    public HttpClientConfiguration(Proxy proxy, String proxyScheme) {
        this.proxy = proxy;
        this.proxyScheme = proxyScheme;
        this.poolOptions = new SharedChannelPoolOptions();
    }

    /**
     * Sets the duration in sec to keep the connection alive in available pool before closing it.
     *
     * @param duration duration in seconds
     * @return HttpClientConfiguration
     */
    @Beta(since = "2.0.0")
    public HttpClientConfiguration withIdleConnectionKeepAliveDurationInSec(long duration) {
        this.poolOptions.withIdleChannelKeepAliveDurationInSec(duration);
        return this;
    }
}
