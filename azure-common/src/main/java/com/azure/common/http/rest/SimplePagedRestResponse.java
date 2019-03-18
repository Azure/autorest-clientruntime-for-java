/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.azure.common.http.rest;

import com.azure.common.http.HttpHeaders;
import com.azure.common.http.HttpRequest;

import java.util.List;

/**
 * REST response with a list of strongly-typed content and link to next page if it exists.
 *
 * @param <T> The deserialized type of the response content.
 */
public class SimplePagedRestResponse<T extends Page<T>> implements RestPagedResponse<T> {
    private final String nextLink;
    private final HttpRequest request;
    private final int statusCode;
    private final HttpHeaders headers;
    private final List<T> items;

    /**
     * Creates RestResponse.
     *
     * @param request the request which resulted in this response
     * @param statusCode the status code of the HTTP response
     * @param headers the headers of the HTTP response
     * @param body the deserialized body
     */
    public SimplePagedRestResponse(HttpRequest request, int statusCode, HttpHeaders headers, T body) {
        this.request = request;
        this.statusCode = statusCode;
        this.headers = headers;
        this.items = body.items();
        this.nextLink = body.nextLink();
    }

    @Override
    public List<T> items() { return items; }

    @Override
    public String nextLink() { return nextLink; }

    /**
     * @return the request which resulted in this RestResponse.
     */
    @Override
    public HttpRequest request() { return request; }

    /**
     * @return the status code of the HTTP response.
     */
    @Override
    public int statusCode() { return statusCode; }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpHeaders headers() { return headers; }

    @Override
    public void close() {
    }
}