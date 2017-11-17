package com.microsoft.rest.v2;

import com.microsoft.rest.v2.http.HttpClient;
import com.microsoft.rest.v2.http.HttpRequest;
import com.microsoft.rest.v2.http.HttpResponse;
import com.microsoft.rest.v2.policy.HttpClientRequestPolicyAdapter;
import com.microsoft.rest.v2.policy.UserAgentPolicy;
import rx.Single;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of RequestPolicies that will be applied to a HTTP request before it is sent and will
 * be applied to a HTTP response when it is received.
 */
public class Pipeline {
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
     * Create a new Pipeline with the provided RequestPolicy factories.
     * @param requestPolicyFactories The RequestPolicy factories to apply to HTTP requests and
     *                               responses that pass through this Pipeline.
     * @param httpClient The HttpClient that this Pipeline will use to send HTTP requests.
     */
    private Pipeline(List<RequestPolicy.Factory> requestPolicyFactories, HttpClient httpClient) {
        this.requestPolicyFactories = requestPolicyFactories;
        this.httpClientRequestPolicyAdapter = new HttpClientRequestPolicyAdapter(httpClient);
    }

    /**
     * Send the provided HTTP request using this Pipeline's HttpClient after it has passed through
     * each of the RequestPolicies that have been configured on this Pipeline.
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
     * Build a new Pipeline that will use the provided HttpClient and RequestPolicy factories.
     * @param httpClient The HttpClient to use.
     * @param requestPolicyFactories The RequestPolicy factories to use.
     * @return The built Pipeline.
     */
    public static Pipeline build(HttpClient httpClient, RequestPolicy.Factory... requestPolicyFactories) {
        final Pipeline.Builder builder = new Pipeline.Builder(httpClient);
        if (requestPolicyFactories != null) {
            for (final RequestPolicy.Factory requestPolicyFactory : requestPolicyFactories) {
                builder.withRequestPolicy(requestPolicyFactory);
            }
        }
        return builder.build();
    }

    /**
     * A builder class that can be used to create a Pipeline.
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
         * Create a new Pipeline builder with no RequestPolicy factories.
         *
         * @param httpClient The HttpClient that will be used to send requests from Pipelines that
         *                   are built from this Builder.
         */
        public Builder(HttpClient httpClient) {
            this.requestPolicyFactories = new ArrayList<>();
            this.httpClient = httpClient;
        }

        /**
         * Add the provided RequestPolicy factory to this Pipeline builder.
         * @param requestPolicyFactory The RequestPolicy factory to add to this Pipeline builder.
         * @return This Pipeline builder.
         */
        public Builder withRequestPolicy(RequestPolicy.Factory requestPolicyFactory) {
            requestPolicyFactories.add(0, requestPolicyFactory);
            return this;
        }

        /**
         * Add a RequestPolicy that will add the providedd UserAgent header to each outgoing
         * HttpRequest.
         * @param userAgent The userAgent header value to add to each outgoing HttpRequest.
         * @return This Pipeline builder.
         */
        public Builder withUserAgent(String userAgent) {
            return withRequestPolicy(new UserAgentPolicy.Factory(userAgent));
        }

        /**
         * Create a new Pipeline from the RequestPolicy factories that have been added to this
         * Pipeline builder.
         * @return The created Pipeline.
         */
        public Pipeline build() {
            return new Pipeline(new ArrayList<>(requestPolicyFactories), httpClient);
        }
    }
}
