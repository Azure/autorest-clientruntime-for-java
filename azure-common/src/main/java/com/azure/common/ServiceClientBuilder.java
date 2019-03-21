package com.azure.common;

import com.azure.common.configuration.ClientConfiguration;
import com.azure.common.credentials.ServiceClientCredentials;
import com.azure.common.http.policy.HttpLogDetailLevel;
import com.azure.common.http.policy.HttpPipelinePolicy;
import com.azure.common.http.policy.RetryPolicy;

import java.util.Objects;

public abstract class ServiceClientBuilder<T extends ServiceClient> {
    private final ClientConfiguration clientConfiguration;

    protected ServiceClientBuilder(ClientConfiguration configuration) {
        this.clientConfiguration = configuration;
    }

    public ServiceClientBuilder withUserAgent(String userAgent) {
        Objects.requireNonNull(userAgent);
        this.clientConfiguration.withUserAgent(userAgent);
        return this;
    }

    public ServiceClientBuilder withRetryPolicy(RetryPolicy policy) {
        Objects.requireNonNull(policy);
        return this;
    }

    public ServiceClientBuilder withCredentials(ServiceClientCredentials credentials) {
        Objects.requireNonNull(credentials);
        this.clientConfiguration.withCredentials(credentials);
        return this;
    }

    public ServiceClientBuilder withHttpLogLevel(HttpLogDetailLevel logLevel) {
        this.clientConfiguration.withHttpLogLevel(logLevel);
        return this;
    }

    public ServiceClientBuilder withPolicy(HttpPipelinePolicy policy) {
        Objects.requireNonNull(policy);
        this.clientConfiguration.addPolicy(policy);
        return this;
    }

    public final T build() {
        return onBuild(clientConfiguration);
    }

    protected abstract T onBuild(ClientConfiguration configuration);
}
