package com.microsoft.rest.v2.policy;

import com.microsoft.rest.v2.RequestPolicy;
import com.microsoft.rest.v2.http.HttpClient;
import com.microsoft.rest.v2.http.HttpRequest;
import com.microsoft.rest.v2.http.HttpResponse;
import rx.Single;

/**
 * An adapter that converts an HttpClient to a RequestPolicy.
 */
public class HttpClientRequestPolicyAdapter implements RequestPolicy {
    private final HttpClient httpClient;

    /**
     * Create a new HttpClientRequestPolicyAdapter that will use the provided HttpClient to send
     * HTTP requests.
     * @param httpClient The HttpClient to use.
     */
    public HttpClientRequestPolicyAdapter(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Single<HttpResponse> sendAsync(HttpRequest request) {
        return httpClient.sendRequestAsync(request);
    }
}
