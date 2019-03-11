/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v3.http.rest;

import com.microsoft.rest.v3.http.HttpHeaders;
import com.microsoft.rest.v3.http.HttpRequest;

import java.util.Map;

/**
 * The response of a REST request.
 *
 * @param <H> The deserialized type of the response headers.
 * @param <T> The deserialized type of the response body.
 */
public class RestResponseBase<H, T> implements RestResponse<T> {
    private final HttpRequest request;
    private final int statusCode;
    private final H customHeaders;
    private final HttpHeaders headers;
    private final T body;

    /**
     * Create RestResponseBase.
     *
     * @param request the request which resulted in this response
     * @param statusCode the status code of the HTTP response
     * @param headers the deserialized headers of the HTTP response
     * @param customHeaders the raw headers of the HTTP response
     * @param body the deserialized body
     */
    public RestResponseBase(HttpRequest request, int statusCode, H customHeaders, HttpHeaders headers,  T body) {
        this.request = request;
        this.statusCode = statusCode;
        this.headers = headers;
        this.customHeaders = customHeaders;
        this.body = body;
    }

    /**
     * @return the request which resulted in this RestResponseBase.
     */
    @Override
    public HttpRequest request() {
        return request;
    }

    /**
     * @return the status code of the HTTP response.
     */
    @Override
    public int statusCode() {
        return statusCode;
    }

    /**
     * @return the deserialized headers of the HTTP response.
     */
    @Override
    public HttpHeaders headers() {
        return headers;
    }

    /**
     * @return a Map containing the raw HTTP response headers.
     */
    public H customHeaders() {
        return customHeaders;
    }

    /**
     * @return the deserialized body of the HTTP response.
     */
    @Override
    public T body() {
        return body;
    }
}
