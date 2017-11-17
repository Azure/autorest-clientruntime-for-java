/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2;

import com.microsoft.rest.v2.http.HttpClient;
import com.microsoft.rest.v2.http.NettyClient;
import com.microsoft.rest.v2.policy.AddCookiesPolicy;
import com.microsoft.rest.v2.policy.LoggingPolicy;
import com.microsoft.rest.v2.policy.RetryPolicy;
import com.microsoft.rest.v2.policy.UserAgentPolicy;
import com.microsoft.rest.v2.protocol.Environment;
import com.microsoft.rest.v2.protocol.SerializerAdapter;
import com.microsoft.rest.v2.serializer.JacksonAdapter;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An immutable configuration object for setting up specific service clients.
 */
public final class RestClient {
    /**
     * A RestClient which provides default values for vanilla clients.
     * Users can modify the default values by calling {@link RestClient#newDefaultBuilder()}.
     */
    public static final RestClient DEFAULT = new RestClient.Builder(new JacksonAdapter()).build();

    /**
     * @return A new {@link RestClient.Builder} instance with default settings for vanilla clients.
     */
    public static RestClient.Builder newDefaultBuilder() {
        return DEFAULT.newBuilder();
    }

    private final HttpClient.Factory httpClientFactory;
    private final HttpClient httpClient;
    private final Proxy proxy;
    private final String baseURL;
    private final String userAgent;
    private final long readTimeoutMillis;
    private final long connectionTimeoutMillis;
    private final SerializerAdapter<?> serializerAdapter;
    private final RequestPolicy.Factory credentialsPolicyFactory;
    private final LogLevel logLevel;

    private final List<RequestPolicy.Factory> customRequestPolicyFactories;

    private RestClient(RestClient.Builder builder) {
        this.proxy = builder.proxy;
        this.baseURL = builder.baseUrl;
        this.userAgent = builder.userAgent;
        this.readTimeoutMillis = builder.readTimeoutMillis;
        this.connectionTimeoutMillis = builder.connectionTimeoutMillis;
        this.serializerAdapter = builder.serializerAdapter;
        this.credentialsPolicyFactory = builder.credentialsPolicyFactory;
        this.logLevel = builder.logLevel;
        this.customRequestPolicyFactories = builder.customRequestPolicyFactories;

        this.httpClientFactory = builder.httpClientFactory;

        List<RequestPolicy.Factory> policyFactories = new ArrayList<>();
        policyFactories.add(new UserAgentPolicy.Factory(userAgent));
        policyFactories.add(new RetryPolicy.Factory());
        policyFactories.add(new AddCookiesPolicy.Factory());
        if (credentialsPolicyFactory != null) {
            policyFactories.add(credentialsPolicyFactory);
        }
        policyFactories.addAll(customRequestPolicyFactories);
        policyFactories.add(new LoggingPolicy.Factory(logLevel));

        HttpClient.Configuration configuration = new HttpClient.Configuration(policyFactories, proxy);
        this.httpClient = httpClientFactory.create(configuration);
    }

    /**
     * @return the user-defined request policy factories.
     */
    public List<RequestPolicy.Factory> customRequestPolicyFactories() {
        return customRequestPolicyFactories;
    }

    /**
     * The adapter for serialization and deserialization.
     * @return the current serializer adapter.
     */
    public SerializerAdapter<?> serializerAdapter() {
        return serializerAdapter;
    }

    /**
     * @return the {@link HttpClient} instance
     */
    public HttpClient httpClient() {
        return httpClient;
    }

    /**
     * @return the {@link Proxy} to use
     */
    public Proxy proxy() {
        return proxy;
    }

    /**
     * The dynamic base URL with variables wrapped in "{" and "}".
     * @return the base URL to make requests to.
     */
    public String baseURL() {
        return baseURL;
    }

    /**
     * @return the connection timeout for HTTP connections in milliseconds.
     */
    public long connectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    /**
     * @return the RequestPolicy.Factory used to add credentials to HTTP requests
     */
    public RequestPolicy.Factory credentialsPolicyFactory() {
        return credentialsPolicyFactory;
    }

    /**
     * @return the current HTTP traffic logging level
     */
    public LogLevel logLevel() {
        return logLevel;
    }

    /**
     * Create a new builder for a new Rest Client with the same configurations on this one.
     * @return a RestClient builder
     */
    public RestClient.Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * @return The user agent string to send in HTTP requests.
     */
    public String userAgent() {
        return userAgent;
    }

    /**
     * @return a new initialized instance of the default SerializerAdapter type.
     */
    public static SerializerAdapter<?> createDefaultSerializer() {
        return new JacksonAdapter();
    }

    /**
     * The builder class for building a REST client.
     */
    public static final class Builder {
        private static final long DEFAULT_READ_TIMEOUT_MILLIS = 10000;
        private static final long DEFAULT_CONNECTION_TIMEOUT_MILLIS = 10000;

        private HttpClient.Factory httpClientFactory;
        private Proxy proxy;
        private String baseUrl;
        private RequestPolicy.Factory credentialsPolicyFactory;

        private List<RequestPolicy.Factory> customRequestPolicyFactories;

        private String userAgent;
        private long readTimeoutMillis;
        private long connectionTimeoutMillis;
        private SerializerAdapter<?> serializerAdapter;
        private LogLevel logLevel;

        private Builder(final RestClient restClient) {
            this.httpClientFactory = restClient.httpClientFactory;
            this.proxy = restClient.proxy;
            this.baseUrl = restClient.baseURL;
            this.userAgent = restClient.userAgent;
            this.connectionTimeoutMillis = restClient.connectionTimeoutMillis;
            this.readTimeoutMillis = restClient.readTimeoutMillis;
            this.serializerAdapter = restClient.serializerAdapter;
            this.credentialsPolicyFactory = restClient.credentialsPolicyFactory;
            this.customRequestPolicyFactories = new ArrayList<>(restClient.customRequestPolicyFactories);
            this.logLevel = restClient.logLevel;
        }

        /**
         * Creates an instance of the builder.
         * @deprecated Use {@link RestClient#newDefaultBuilder} or AzureRestClient.newDefaultBuilder if using the Azure runtime.
         */
        @Deprecated
        public Builder() {
            this(RestClient.DEFAULT);
        }

        /**
         * Creates an instance of the builder with required parameters.
         * @param serializerAdapter The serializer adapter to use.
         */
        public Builder(SerializerAdapter<?> serializerAdapter) {
            this.serializerAdapter = serializerAdapter;
            httpClientFactory = new NettyClient.Factory();
            connectionTimeoutMillis = DEFAULT_CONNECTION_TIMEOUT_MILLIS;
            readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS;
            customRequestPolicyFactories = new ArrayList<>();
            logLevel = LogLevel.NONE;
        }

        /**
         * Sets the httpClientFactory.
         * @param httpClientFactory the httpClientFactory to use.
         * @return the builder itself for chaining.
         */
        public Builder withHttpClientFactory(HttpClient.Factory httpClientFactory) {
            this.httpClientFactory = httpClientFactory;
            return this;
        }

        /**
         * Sets the proxy.
         * If the proxy requires authentication, also add a ProxyAuthenticationPolicy.Factory
         * with {@link #addRequestPolicy(RequestPolicy.Factory)}.
         *
         * @param proxy the proxy to use.
         * @return the builder itself for chaining.
         */
        public Builder withProxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        /**
         * Sets the dynamic base URL.
         *
         * @param baseUrl the base URL to use.
         * @return the builder itself for chaining.
         */
        public Builder withBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the base URL with the default from the Environment.
         *
         * @param environment the environment to use
         * @param endpoint the environment endpoint the application is accessing
         * @return the builder itself for chaining
         */
        public Builder withBaseUrl(Environment environment, Environment.Endpoint endpoint) {
            this.baseUrl = environment.url(endpoint);
            return this;
        }

        /**
         * Sets the serialization adapter.
         *
         * @param serializerAdapter the adapter to a serializer
         * @return the builder itself for chaining
         */
        public Builder withSerializerAdapter(SerializerAdapter<?> serializerAdapter) {
            this.serializerAdapter = serializerAdapter;
            return this;
        }

        /**
         * Sets the RequestPolicy.Factory for adding credentials to HTTP requests.
         *
         * @param credentialsPolicyFactory The RequestPolicy.Factory for adding credentials to HTTP requests.
         * @return the builder itself for chaining.
         */
        public Builder withCredentialsPolicy(RequestPolicy.Factory credentialsPolicyFactory) {
            this.credentialsPolicyFactory = credentialsPolicyFactory;
            return this;
        }

        /**
         * Sets the user agent header.
         *
         * @param userAgent the user agent header.
         * @return the builder itself for chaining.
         */
        public Builder withUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Sets the HTTP log level.
         *
         * @param logLevel the {@link LogLevel} enum.
         * @return the builder itself for chaining.
         */
        public Builder withLogLevel(LogLevel logLevel) {
            if (logLevel == null) {
                throw new NullPointerException("logLevel == null");
            }
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Set the read timeout on the HTTP client. Default is 10 seconds.
         *
         * @param timeout the timeout numeric value
         * @param unit the time unit for the numeric value
         * @return the builder itself for chaining
         */
        public Builder withReadTimeout(long timeout, TimeUnit unit) {
            readTimeoutMillis = unit.toMillis(timeout);
            return this;
        }

        /**
         * Set the connection timeout on the HTTP client. Default is 10 seconds.
         *
         * @param timeout the timeout numeric value
         * @param unit the time unit for the numeric value
         * @return the builder itself for chaining
         */
        public Builder withConnectionTimeout(long timeout, TimeUnit unit) {
            connectionTimeoutMillis = unit.toMillis(timeout);
            return this;
        }

        /**
         * Adds a custom RequestPolicy.Factory to the request pipeline in addition to the standard policies.
         *
         * @param factory The Factory producing a custom user-defined RequestPolicy.
         * @return the builder itself for chaining
         */
        public Builder addRequestPolicy(RequestPolicy.Factory factory) {
            customRequestPolicyFactories.add(factory);
            return this;
        }

        /**
         * Sets the list of custom RequestPolicy.Factory objects.
         * Does not affect creation of standard policies e.g. AddCookiesPolicy, LoggingPolicy, ...
         *
         * @param factories The list of factories producing custom user-defined RequestPolicies.
         * @return the builder itself for chaining
         */
        public Builder setRequestPolicies(List<RequestPolicy.Factory> factories) {
            this.customRequestPolicyFactories = factories;
            return this;
        }

        /**
         * Build a RestClient with all the current configurations.
         *
         * @return a {@link RestClient}.
         */
        public RestClient build() {
            if (serializerAdapter == null) {
                throw new NullPointerException("Please set serializer adapter.");
            }

            return new RestClient(this);
        }
    }
}
