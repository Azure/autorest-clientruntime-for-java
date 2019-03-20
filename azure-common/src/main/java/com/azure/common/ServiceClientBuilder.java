package com.azure.common;

import com.azure.common.configuration.ClientConfiguration;
import com.azure.common.credentials.ServiceClientCredentials;
import com.azure.common.exceptions.InvalidConfigurationException;
import com.azure.common.http.policy.HttpPipelinePolicy;
import com.azure.common.http.policy.RetryPolicy;

import java.util.Objects;

public abstract class ServiceClientBuilder<T extends ServiceClient> {
    private ClientConfiguration clientConfiguration;

    protected ServiceClientBuilder() {
        this.clientConfiguration = new ClientConfiguration();
    }

    public ServiceClientBuilder withUserAgent(String userAgent) {
        Objects.requireNonNull(userAgent);
        this.clientConfiguration.setUserAgent(userAgent);
        return this;
    }

    public ServiceClientBuilder withRetryPolicy(RetryPolicy policy) {
        Objects.requireNonNull(policy);
        return this;
    }

    public ServiceClientBuilder withCredentials(ServiceClientCredentials credentials) {
        Objects.requireNonNull(credentials);
        this.clientConfiguration.setCredentials(credentials);
        return this;
    }

    public ServiceClientBuilder withPolicy(HttpPipelinePolicy policy) {
        Objects.requireNonNull(policy);
        this.clientConfiguration.addPolicy(policy);
        return this;
    }

    public T build() {
        validateConfiguration(clientConfiguration);
        return onBuild(clientConfiguration);
    }

    public ClientConfiguration defaultConfig() {
        return new ClientConfiguration();
    }

    protected abstract T onBuild(ClientConfiguration configuration);

    protected abstract void validateConfiguration(ClientConfiguration configuration) throws InvalidConfigurationException;
}
