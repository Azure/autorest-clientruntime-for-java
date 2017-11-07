/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.v2;

import com.microsoft.rest.v2.RestProxy;
import com.microsoft.rest.v2.SwaggerMethodParser;
import com.microsoft.rest.v2.http.HttpRequest;
import com.microsoft.rest.v2.http.HttpResponse;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.lang.reflect.Type;

/**
 * The "polling strategy" that is used when a request completes immediately and does not require any
 * further polling.
 */
public class CompletedPollStrategy extends PollStrategy {
    private final HttpResponse bufferedOriginalHttpResponse;

    /**
     * Create a new CompletedPollStrategy.
     * @param restProxy The RestProxy that created this PollStrategy.
     * @param methodParser The method parser that describes the service interface method that
     *                     initiated the long running operation.
     * @param originalHttpResponse The HTTP response to the original HTTP request.
     */
    public CompletedPollStrategy(RestProxy restProxy, SwaggerMethodParser methodParser, HttpResponse originalHttpResponse) {
        super(restProxy, methodParser, 0);

        this.bufferedOriginalHttpResponse = originalHttpResponse.buffer();
        setStatus(OperationState.SUCCEEDED);
    }

    @Override
    HttpRequest createPollRequest() {
        return null;
    }

    @Override
    Single<HttpResponse> updateFromAsync(HttpResponse httpPollResponse) {
        return null;
    }

    @Override
    boolean isDone() {
        return true;
    }

    Observable<OperationStatus<Object>> pollUntilDoneWithStatusUpdates(final HttpRequest originalHttpRequest, final SwaggerMethodParser methodParser, final Type operationStatusResultType) {
        return createOperationStatusObservable(originalHttpRequest, bufferedOriginalHttpResponse, methodParser, operationStatusResultType);
    }

    Single<HttpResponse> pollUntilDone() {
        return Single.just(bufferedOriginalHttpResponse);
    }
}
