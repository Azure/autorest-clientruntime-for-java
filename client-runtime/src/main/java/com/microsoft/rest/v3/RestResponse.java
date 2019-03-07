/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v3;

import com.microsoft.rest.v3.http.HttpRequest;

import java.util.Map;

/**
 * REST response with a strongly-typed content specified.
 *
 * @param <TBody> The deserialized type of the response content.
 */
public final class RestResponse<TBody> extends RestResponseBase<Void, TBody> {
    /**
     * Creates RestResponse.
     *
     * @param request the request which resulted in this response
     * @param statusCode the status code of the HTTP response
     * @param rawHeaders the raw headers of the HTTP response
     * @param body the deserialized body
     */
    public RestResponse(HttpRequest request, int statusCode, Map<String, String> rawHeaders, TBody body) {
        super(request, statusCode, null, rawHeaders, body);
    }

    // Used for uniform reflective creation in RestProxy.
    @SuppressWarnings("unused")
    RestResponse(HttpRequest request, int statusCode, Void headers, Map<String, String> rawHeaders, TBody body) {
        super(request, statusCode, headers, rawHeaders, body);
    }

    /**
     * @return the deserialized body of the HTTP response
     */
    public TBody body() {
        return super.body();
    }
}
