/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.http;

import com.microsoft.rest.policy.RequestPolicy;
import rx.Single;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A generic interface for sending HTTP requests and getting responses.
 */
public abstract class HttpClient {
    private final List<RequestPolicy.Factory> policyFactories;

    private final RequestPolicy lastRequestPolicy = new RequestPolicy() {
        @Override
        public Single<HttpResponse> sendAsync(HttpRequest request) {
            return sendRequestInternalAsync(request);
        }
    };

    protected HttpClient() {
        this.policyFactories = Collections.emptyList();
    }

    protected HttpClient(List<? extends RequestPolicy.Factory> policyFactories) {
        this.policyFactories = new ArrayList<>(policyFactories);

        // Reversing the list facilitates the creation of the RequestPolicy linked list per-request.
        Collections.reverse(this.policyFactories);
    }

    /**
     * Send the provided request asynchronously, applying any request policies provided to the HttpClient instance.
     * @param request The HTTP request to send.
     * @return A {@link Single} representing the HTTP response that will arrive asynchronously.
     */
    public final Single<HttpResponse> sendRequestAsync(HttpRequest request) {
        // Builds a linked list starting from the end.
        RequestPolicy next = lastRequestPolicy;
        for (RequestPolicy.Factory factory : policyFactories) {
            next = factory.create(next);
        }
        return next.sendAsync(request);
    }

    /**
     * Send the provided request asynchronously through the concrete HTTP client implementation.
     * @param request The HTTP request to send.
     * @return A {@link Single} representing the HTTP response that will arrive asynchronously.
     */
    protected abstract Single<HttpResponse> sendRequestInternalAsync(HttpRequest request);
}
