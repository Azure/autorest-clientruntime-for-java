package com.azure.common.configuration;

import com.azure.common.credentials.ServiceClientCredentials;
import com.azure.common.http.policy.HttpPipelinePolicy;
import com.azure.common.http.policy.RetryPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClientConfiguration {
    private ServiceClientCredentials credentials;
    private String userAgent;
    private RetryPolicy retryPolicy;
    private List<HttpPipelinePolicy> policies;

    /**
     * Gets the default configuration settings.
     */
    public ClientConfiguration() {
        this.retryPolicy = new RetryPolicy();
        this.policies = new ArrayList<>();
    }

    public ServiceClientCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(ServiceClientCredentials credentials) {
        Objects.requireNonNull(credentials);
        this.credentials = credentials;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        Objects.requireNonNull(userAgent);
        this.userAgent = userAgent;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        Objects.requireNonNull(retryPolicy);
        this.retryPolicy = retryPolicy;
    }

    public List<HttpPipelinePolicy> getPolicies() {
        return policies;
    }

    public void addPolicy(HttpPipelinePolicy policy) {
        Objects.requireNonNull(policy);
        this.policies.add(policy);
    }

}
