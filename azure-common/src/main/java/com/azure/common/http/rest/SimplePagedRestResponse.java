/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.azure.common.http.rest;

import com.azure.common.http.HttpHeaders;
import com.azure.common.http.HttpRequest;

import java.util.List;

public class SimplePagedRestResponse<T> extends SimpleRestResponse<List<T>> implements RestPagedResponse<T> {
    private final String nextLink;

    /**
     * Creates RestResponse.
     *
     * @param request    the request which resulted in this response
     * @param statusCode the status code of the HTTP response
     * @param headers    the headers of the HTTP response
     * @param body       the deserialized body
     */
    public SimplePagedRestResponse(HttpRequest request, int statusCode, HttpHeaders headers, List<T> body, String nextLink) {
        super(request, statusCode, headers, body);
        this.nextLink = nextLink;
    }

    @Override
    public List<T> items() {
        return body();
    }

    @Override
    public String nextLink() {
        return nextLink;
    }

    @Override
    public void close() {
    }
}