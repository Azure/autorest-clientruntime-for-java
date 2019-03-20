package com.azure.common;

import com.azure.common.configuration.ConfigurationSettings;
import com.azure.common.credentials.ServiceClientCredentials;
import com.azure.common.exceptions.InvalidConfigurationException;
import com.azure.common.http.policy.HttpPipelinePolicy;
import com.azure.common.http.policy.RetryPolicy;

import java.util.Objects;

public abstract class ServiceClientBuilder<T extends ServiceClient> {
    private ConfigurationSettings configurationSettings;

    protected ServiceClientBuilder() {
        this.configurationSettings = new ConfigurationSettings();
    }

    public ServiceClientBuilder withUserAgent(String userAgent) {
        Objects.requireNonNull(userAgent);
        this.configurationSettings.setUserAgent(userAgent);
        return this;
    }

    public ServiceClientBuilder withRetryPolicy(RetryPolicy policy) {
        Objects.requireNonNull(policy);
        return this;
    }

    public ServiceClientBuilder withCredentials(ServiceClientCredentials credentials) {
        Objects.requireNonNull(credentials);
        this.configurationSettings.setCredentials(credentials);
        return this;
    }

    public ServiceClientBuilder withPolicy(HttpPipelinePolicy policy) {
        Objects.requireNonNull(policy);
        this.configurationSettings.addPolicy(policy);
        return this;
    }

    public T build() {
        validateConfiguration(configurationSettings);
        return onBuild(configurationSettings);
    }

    public ConfigurationSettings defaultConfig() {
        return new ConfigurationSettings();
    }

    protected abstract T onBuild(ConfigurationSettings configuration);

    protected abstract void validateConfiguration(ConfigurationSettings configuration) throws InvalidConfigurationException;
}
