package com.azure.common;

import com.azure.common.configuration.ClientConfiguration;
import com.azure.common.credentials.AsyncServiceClientCredentials;
import com.azure.common.http.policy.HttpLogDetailLevel;
import com.azure.common.http.policy.HttpPipelinePolicy;
import com.azure.common.http.policy.RetryPolicy;
import org.slf4j.ILoggerFactory;

import java.util.Objects;

public abstract class ServiceClientBuilder<T extends ServiceClient> {
    private final ClientConfiguration clientConfiguration;

    protected ServiceClientBuilder(ClientConfiguration configuration) {
        clientConfiguration = configuration;
    }

    public ServiceClientBuilder withUserAgent(String userAgent) {
        Objects.requireNonNull(userAgent);
        clientConfiguration.withUserAgent(userAgent);
        return this;
    }

    public ServiceClientBuilder withRetryPolicy(RetryPolicy policy) {
        Objects.requireNonNull(policy);
        return this;
    }

    public ServiceClientBuilder withCredentials(AsyncServiceClientCredentials credentials) {
        Objects.requireNonNull(credentials);
        clientConfiguration.withCredentials(credentials);
        return this;
    }

    public ServiceClientBuilder withHttpLogLevel(HttpLogDetailLevel logLevel) {
        clientConfiguration.withHttpLogLevel(logLevel);
        return this;
    }

    public ServiceClientBuilder withPolicy(HttpPipelinePolicy policy) {
        Objects.requireNonNull(policy);
        clientConfiguration.addPolicy(policy);
        return this;
    }

    public ServiceClientBuilder withLoggerFactory(ILoggerFactory loggerFactory) {
        Objects.requireNonNull(loggerFactory);
        clientConfiguration.withLoggerFactory(loggerFactory);
        return this;
    }

    public final T build() {
        return onBuild(clientConfiguration);
    }

    protected abstract T onBuild(ClientConfiguration configuration);
}
