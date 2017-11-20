package com.microsoft.rest.v2.http;

import com.microsoft.rest.v2.policy.RequestPolicy;
import com.microsoft.rest.v2.policy.HttpClientRequestPolicyAdapter;
import com.microsoft.rest.v2.policy.UserAgentPolicy;
import rx.Single;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of RequestPolicies that will be applied to a HTTP request before it is sent and will
 * be applied to a HTTP response when it is received.
 */
public class HttpPipeline {
    /**
     * The list of RequestPolicy factories that will be applied to HTTP requests and responses.
     * The factories appear in this list in the order that they will be applied to outgoing
     * requests.
     */
    private final List<RequestPolicy.Factory> requestPolicyFactories;

    /**
     * The HttpClient that will be used to send requests unless the sendRequestAsync() method is
     * called with a different HttpClient.
     */
    private HttpClientRequestPolicyAdapter httpClientRequestPolicyAdapter;

    /**
     * Create a new HttpPipeline with the provided RequestPolicy factories.
     * @param requestPolicyFactories The RequestPolicy factories to apply to HTTP requests and
     *                               responses that pass through this HttpPipeline.
     * @param httpClient The HttpClient that this HttpPipeline will use to send HTTP requests.
     */
    private HttpPipeline(List<RequestPolicy.Factory> requestPolicyFactories, HttpClient httpClient) {
        this.requestPolicyFactories = requestPolicyFactories;
        this.httpClientRequestPolicyAdapter = new HttpClientRequestPolicyAdapter(httpClient);
    }

    /**
     * Send the provided HTTP request using this HttpPipeline's HttpClient after it has passed through
     * each of the RequestPolicies that have been configured on this HttpPipeline.
     * @param httpRequest The HttpRequest to send.
     * @return The HttpResponse that was received.
     */
    public Single<HttpResponse> sendRequestAsync(HttpRequest httpRequest) {
        RequestPolicy requestPolicy = httpClientRequestPolicyAdapter;
        for (final RequestPolicy.Factory requestPolicyFactory : requestPolicyFactories) {
            requestPolicy = requestPolicyFactory.create(requestPolicy);
        }
        return requestPolicy.sendAsync(httpRequest);
    }

    /**
     * Build a new HttpPipeline that will use the provided HttpClient and RequestPolicy factories.
     * @param httpClient The HttpClient to use.
     * @param requestPolicyFactories The RequestPolicy factories to use.
     * @return The built HttpPipeline.
     */
    public static HttpPipeline build(HttpClient httpClient, RequestPolicy.Factory... requestPolicyFactories) {
        final HttpPipeline.Builder builder = new HttpPipeline.Builder(httpClient);
        if (requestPolicyFactories != null) {
            for (final RequestPolicy.Factory requestPolicyFactory : requestPolicyFactories) {
                builder.withRequestPolicy(requestPolicyFactory);
            }
        }
        return builder.build();
    }

    /**
     * A builder class that can be used to create a HttpPipeline.
     */
    public static class Builder {
        /**
         * The list of RequestPolicy factories that will be applied to HTTP requests and responses.
         * The factories appear in this list in the reverse order that they will be applied to
         * outgoing requests.
         */
        private final List<RequestPolicy.Factory> requestPolicyFactories;

        /**
         * The HttpClient that will be used to send requests from Pipelines that are built from this
         * Builder.
         */
        private final HttpClient httpClient;

        /**
         * Create a new HttpPipeline builder with no RequestPolicy factories.
         *
         * @param httpClient The HttpClient that will be used to send requests from Pipelines that
         *                   are built from this Builder.
         */
        public Builder(HttpClient httpClient) {
            this.requestPolicyFactories = new ArrayList<>();
            this.httpClient = httpClient;
        }

        /**
         * Add the provided RequestPolicy factory to this HttpPipeline builder.
         * @param requestPolicyFactory The RequestPolicy factory to add to this HttpPipeline builder.
         * @return This HttpPipeline builder.
         */
        public Builder withRequestPolicy(RequestPolicy.Factory requestPolicyFactory) {
            requestPolicyFactories.add(0, requestPolicyFactory);
            return this;
        }

        /**
         * Add a RequestPolicy that will add the providedd UserAgent header to each outgoing
         * HttpRequest.
         * @param userAgent The userAgent header value to add to each outgoing HttpRequest.
         * @return This HttpPipeline builder.
         */
        public Builder withUserAgent(String userAgent) {
            return withRequestPolicy(new UserAgentPolicy.Factory(userAgent));
        }

        /**
         * Create a new HttpPipeline from the RequestPolicy factories that have been added to this
         * HttpPipeline builder.
         * @return The created HttpPipeline.
         */
        public HttpPipeline build() {
            return new HttpPipeline(new ArrayList<>(requestPolicyFactories), httpClient);
        }
    }
}
