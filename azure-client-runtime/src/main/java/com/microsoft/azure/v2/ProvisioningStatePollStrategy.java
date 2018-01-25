/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.v2;

import com.microsoft.rest.v2.RestProxy;
import com.microsoft.rest.v2.SwaggerMethodParser;
import com.microsoft.rest.v2.http.HttpMethod;
import com.microsoft.rest.v2.http.HttpRequest;
import com.microsoft.rest.v2.http.HttpResponse;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import java.io.IOException;

/**
 * A PollStrategy that will continue to poll a resource's URL until the resource's provisioning
 * state property is in a completed state.
 */
public class ProvisioningStatePollStrategy extends PollStrategy {
    private final HttpRequest originalRequest;
    private final SwaggerMethodParser methodParser;

    ProvisioningStatePollStrategy(RestProxy restProxy, SwaggerMethodParser methodParser, HttpRequest originalRequest, String provisioningState, long delayInMilliseconds) {
        super(restProxy, methodParser, delayInMilliseconds);

        this.originalRequest = originalRequest;
        this.methodParser = methodParser;
        setStatus(provisioningState);
    }

    @Override
    HttpRequest createPollRequest() {
        return new HttpRequest(originalRequest.callerMethod(), HttpMethod.GET, originalRequest.url());
    }

    @Override
    Single<HttpResponse> updateFromAsync(HttpResponse pollResponse) {
        ResourceWithProvisioningState resource = null;
        try {
            resource = reserialize(pollResponse.deserializedBody(), ResourceWithProvisioningState.class);
        } catch (IOException ignored) {
        }

        if (resource == null || resource.properties() == null || resource.properties().provisioningState() == null) {
            if (methodParser.isExpectedResponseStatusCode(pollResponse.statusCode())) {
                setStatus(OperationState.SUCCEEDED);
            } else {
                setStatus(OperationState.FAILED);
            }
        }
        else if (OperationState.isFailedOrCanceled(resource.properties().provisioningState())) {
            throw new CloudException("Async operation failed with provisioning state: " + resource.properties().provisioningState(), pollResponse);
        }
        else {
            setStatus(resource.properties().provisioningState());
        }

        return Single.just(pollResponse);
    }

    @Override
    boolean isDone() {
        return OperationState.isCompleted(status());
    }
}