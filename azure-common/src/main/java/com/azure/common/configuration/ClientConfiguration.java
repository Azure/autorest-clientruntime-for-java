package com.azure.common.configuration;

import com.azure.common.credentials.AsyncServiceClientCredentials;
import com.azure.common.http.HttpClient;
import com.azure.common.http.policy.HttpLogDetailLevel;
import com.azure.common.http.policy.HttpPipelinePolicy;
import com.azure.common.http.policy.RetryPolicy;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClientConfiguration {
    private HttpClient client;
    private AsyncServiceClientCredentials credentials;
    private String userAgent;
    private RetryPolicy retryPolicy;
    private List<HttpPipelinePolicy> policies;
    private HttpLogDetailLevel httpLogDetailLevel;
    private ILoggerFactory loggerFactory;
    private URL serviceEndpoint;

    /**
     * Gets the default configuration settings
     */
    public ClientConfiguration() {
        this.retryPolicy = new RetryPolicy();
        this.policies = new ArrayList<>();
        this.httpLogDetailLevel = HttpLogDetailLevel.NONE;
        this.loggerFactory = LoggerFactory.getILoggerFactory();
    }

    public AsyncServiceClientCredentials getCredentials() {
        return credentials;
    }

    public ClientConfiguration withCredentials(AsyncServiceClientCredentials credentials) {
        Objects.requireNonNull(credentials);
        this.credentials = credentials;
        return this;
    }

    public String userAgent() {
        return userAgent;
    }

    public ClientConfiguration withUserAgent(String userAgent) {
        Objects.requireNonNull(userAgent);
        this.userAgent = userAgent;
        return this;
    }

    public RetryPolicy retryPolicy() {
        return retryPolicy;
    }

    public ClientConfiguration withRetryPolicy(RetryPolicy retryPolicy) {
        Objects.requireNonNull(retryPolicy);
        this.retryPolicy = retryPolicy;
        return this;
    }

    public List<HttpPipelinePolicy> getPolicies() {
        return policies;
    }

    public ClientConfiguration addPolicy(HttpPipelinePolicy policy) {
        Objects.requireNonNull(policy);
        this.policies.add(policy);
        return this;
    }

    public HttpClient getHttpClient() {
        return client;
    }

    public ClientConfiguration setHttpClient(HttpClient client) {
        Objects.requireNonNull(client);
        this.client = client;
        return this;
    }

    public HttpLogDetailLevel httpLogDetailLevel() { return httpLogDetailLevel; }

    public ClientConfiguration withHttpLogLevel(HttpLogDetailLevel logLevel) {
        this.httpLogDetailLevel = logLevel;
        return this;
    }

    public ILoggerFactory loggerFactory() { return loggerFactory; }

    public ClientConfiguration withLoggerFactory(ILoggerFactory loggerFactory) {
        Objects.requireNonNull(loggerFactory);
        this.loggerFactory = loggerFactory;
        return this;
    }

    public URL serviceEndpoint() { return serviceEndpoint; }

    public ClientConfiguration withServiceEndpoint(String serviceEndpoint) {
        Objects.requireNonNull(serviceEndpoint);

        if (serviceEndpoint.equals("")) {
            throw new IllegalArgumentException("'serviceEndpoint' cannot be empty.");
        }

        try {
            this.serviceEndpoint = new URL(serviceEndpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("'serviceEndpoint' is not a valid URL.", e);
        }

        return this;
    }
}
