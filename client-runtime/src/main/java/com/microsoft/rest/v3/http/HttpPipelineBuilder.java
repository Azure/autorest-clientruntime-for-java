/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v3.http;

import com.microsoft.rest.v3.credentials.ServiceClientCredentials;
import com.microsoft.rest.v3.policy.CookiePolicy;
import com.microsoft.rest.v3.policy.CredentialsPolicy;
import com.microsoft.rest.v3.policy.DecodingPolicy;
import com.microsoft.rest.v3.policy.HostPolicy;
import com.microsoft.rest.v3.policy.HttpLogDetailLevel;
import com.microsoft.rest.v3.policy.HttpLoggingPolicy;
import com.microsoft.rest.v3.policy.HttpPipelinePolicy;
import com.microsoft.rest.v3.policy.ProxyAuthenticationPolicy;
import com.microsoft.rest.v3.policy.RequestIdPolicy;
import com.microsoft.rest.v3.policy.RequestPolicyOptions;
import com.microsoft.rest.v3.policy.RetryPolicy;
import com.microsoft.rest.v3.policy.TimeoutPolicy;
import com.microsoft.rest.v3.policy.UserAgentPolicy;
import com.microsoft.rest.v3.protocol.HttpResponseDecoder;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * A builder class that can be used to create a HttpPipeline.
 */
public class HttpPipelineBuilder {
    /**
     * The optional properties that will be set on the created HTTP pipelines.
     */
    private HttpPipelineOptions pipelineOptions;

    /**
     * The list of policies that will be applied to HTTP requests and responses.
     */
    private final List<HttpPipelinePolicy> pipelinePolicies;

    /**
     * Create a new HttpPipeline builder.
     */
    public HttpPipelineBuilder() {
        this(null);
    }

    /**
     * Create a new HttpPipeline builder.
     *
     * @param pipelineOptions The optional properties that will be set on the created HTTP pipelines.
     */
    public HttpPipelineBuilder(HttpPipelineOptions pipelineOptions) {
        this.pipelineOptions = pipelineOptions;
        this.pipelinePolicies = new ArrayList<>();
    }

    /**
     * Get the policies HttpPipeline builder.
     *
     * @return the policies in this HttpPipeline builder.
     */
    List<HttpPipelinePolicy> pipelinePolicies() {
        return pipelinePolicies;
    }

    /**
     * @return the pipelineOptions for this HttpPipeline builder.
     */
    HttpPipelineOptions options() {
        return pipelineOptions;
    }

    /**
     * Set the HttpClient that will be used by HttpPipelines that are created by this Builder.
     *
     * @param httpClient The HttpClient to use.
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withHttpClient(HttpClient httpClient) {
        if (pipelineOptions == null) {
            pipelineOptions = new HttpPipelineOptions();
        }
        pipelineOptions.withHttpClient(httpClient);
        return this;
    }

    /**
     * Set the Logger that will be used for each HttpPipelinePolicy within the created HttpPipeline.
     *
     * @param logger The Logger to provide to each HttpPipelinePolicy.
     * @return This HttpPipeline pipelineOptions object.
     */
    public HttpPipelineBuilder withLogger(HttpPipelineLogger logger) {
        if (pipelineOptions == null) {
            pipelineOptions = new HttpPipelineOptions();
        }
        pipelineOptions.withLogger(logger);
        return this;
    }

    /**
     * Add the provided HttpPipelinePolicy factory to this HttpPipeline builder.
     *
     * @param requestPolicy The request policy to add to this HttpPipeline builder.
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withPolicy(HttpPipelinePolicy requestPolicy) {
        return withPolicy(pipelinePolicies.size(), requestPolicy);
    }

    /**
     * Add the provided HttpPipelinePolicy factory to this HttpPipeline builder
     * at the provided index in the pipeline.
     *
     * @param index The index to insert the provided HttpPipelinePolicy factory.
     * @param requestPolicy The request policy to add to this HttpPipeline builder.
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withPolicy(int index, HttpPipelinePolicy requestPolicy) {
        pipelinePolicies.add(index, requestPolicy);
        return this;
    }

    /**
     * Add the provided HttpPipelinePolicy factories to this HttpPipeline builder.
     *
     * @param requestPolicies The request policies add to this HttpPipeline builder.
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withPolicies(HttpPipelinePolicy... requestPolicies) {
        for (HttpPipelinePolicy policy : requestPolicies) {
            withPolicy(policy);
        }
        return this;
    }

    /**
     * Add a HttpPipelinePolicy which stores and adds cookies across multiple
     * requests and responses.
     *
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withCookiePolicy() {
        return withPolicy(new CookiePolicy());
    }

    /**
     * Add a HttpPipelinePolicy which applies the given ServiceClientCredentials to
     * outgoing requests.
     *
     * @param credentials The credentials to apply to requests.
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withCredentialsPolicy(ServiceClientCredentials credentials) {
        return withPolicy(new CredentialsPolicy(credentials));
    }

    /**
     * Adds a HttpPipelinePolicy which decodes the headers and body of incoming
     * responses.
     * Required for services that need to deserialize JSON or XML responses.
     *
     * @param decoder decoder to deserialize JSON or XML responses
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withDecodingPolicy(HttpResponseDecoder decoder) {
        return withPolicy(new DecodingPolicy(decoder));
    }

    /**
     * Adds a HttpPipelinePolicy which sets the host on all outgoing requests.
     *
     * @param host The hostname to use in all outgoing requests.
     * @return This HttpPipelineBuilder.
     */
    public HttpPipelineBuilder withHostPolicy(String host) {
        return withPolicy(new HostPolicy(host));
    }

    /**
     * Adds a HttpPipelinePolicy which logs all HTTP traffic using SLF4J.
     *
     * @param level The HTTP logging detail level.
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withHttpLoggingPolicy(HttpLogDetailLevel level) {
        return withHttpLoggingPolicy(level, false);
    }

    /**
     * Adds a HttpPipelinePolicy which logs all HTTP traffic using SLF4J.
     *
     * @param level The HTTP logging detail level.
     * @param prettyPrintJSON Whether or not to pretty print JSON message bodies.
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withHttpLoggingPolicy(HttpLogDetailLevel level, boolean prettyPrintJSON) {
        return withPolicy(new HttpLoggingPolicy(level, prettyPrintJSON));
    }

    /**
     * Adds a HttpPipelinePolicy which adds proxy authentication headers to
     * outgoing requests.
     *
     * @param username The username for authentication.
     * @param password The password for authentication.
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withProxyAuthenticationPolicy(String username, String password) {
        return withPolicy(new ProxyAuthenticationPolicy(username, password));
    }

    /**
     * Adds a HttpPipelinePolicy which adds a per-request ID to the
     * "x-ms-client-request-id" header to outgoing requests.
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withRequestIdPolicy() {
        return withPolicy(new RequestIdPolicy());
    }

    /**
     * Adds a HttpPipelinePolicy which retries a failed request up to the given
     * number of times.
     *
     * @param maxRetries The maximum number of times to retry failed requests.
     * @param delayTime the delay between retries
     * @param timeUnit the time unit of the delay
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withRetryPolicy(int maxRetries, long delayTime, ChronoUnit timeUnit) {
        return withPolicy(new RetryPolicy(maxRetries, delayTime, timeUnit));
    }

    /**
     * Adds a HttpPipelinePolicy which fails a request if it does not complete by
     * the time the given interval elapses.
     *
     * @param timeout The amount of time to wait before timing out a request.
     * @param unit The unit of time associated with the timeout parameter.
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withTimeoutPolicy(long timeout, ChronoUnit unit) {
        return withPolicy(new TimeoutPolicy(timeout, unit));
    }

    /**
     * Add a HttpPipelinePolicy that will add the provided UserAgent header to each
     * outgoing HttpRequest.
     *
     * @param userAgent The userAgent header value to add to each outgoing HttpRequest.
     * @return This HttpPipeline builder.
     */
    public HttpPipelineBuilder withUserAgentPolicy(String userAgent) {
        return withPolicy(new UserAgentPolicy(userAgent));
    }

    /**
     * Create a new HttpPipeline from the HttpPipelinePolicy factories that have been added to this
     * HttpPipeline builder.
     *
     * @return The created HttpPipeline.
     */
    public HttpPipeline build() {
        final int policyCount = pipelinePolicies.size();
        final HttpPipelinePolicy[] requestPolicyArray = new HttpPipelinePolicy[policyCount];
        //
        HttpClient httpClient = (pipelineOptions == null || pipelineOptions.httpClient() == null)
                ? HttpClient.createDefault()
                : pipelineOptions.httpClient();
        //
        return new HttpPipeline(pipelinePolicies.toArray(requestPolicyArray),
                httpClient,
                new RequestPolicyOptions(pipelineOptions == null ? null : pipelineOptions.logger()));
    }
}
