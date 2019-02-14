/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v3.policy;

import com.microsoft.rest.v3.http.HttpPipelineCallContext;
import com.microsoft.rest.v3.http.HttpPipelineLogLevel;
import com.microsoft.rest.v3.http.HttpResponse;
import com.microsoft.rest.v3.http.NextPolicy;
import com.microsoft.rest.v3.http.UrlBuilder;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;

/**
 * The Pipeline policy that adds the given host to each HttpRequest.
 */
public class HostPolicy extends AbstractPipelinePolicy {
    private final String host;

    /**
     * Create HostPolicy.
     *
     * @param host The host to set on every HttpRequest.
     */
    public HostPolicy(String host) {
        this(host, RequestPolicyOptions.NULL_REQUEST_POLICY_OPTIONS);
    }

    /**
     * Create HostPolicy.
     *
     * @param host The host to set on every HttpRequest.
     * @param options the request options
     */
    public HostPolicy(String host, RequestPolicyOptions options) {
        super(options);
        this.host = host;
    }

    @Override
    public Mono<HttpResponse> process(HttpPipelineCallContext context, NextPolicy next) {
        if (shouldLog(HttpPipelineLogLevel.INFO)) {
            log(HttpPipelineLogLevel.INFO, "Setting host to {0}", host);
        }

        Mono<HttpResponse> result;
        final UrlBuilder urlBuilder = UrlBuilder.parse(context.httpRequest().url());
        try {
            context.httpRequest().withUrl(urlBuilder.withHost(host).toURL());
            result = next.process();
        } catch (MalformedURLException e) {
            result = Mono.error(e);
        }
        return result;
    }
}