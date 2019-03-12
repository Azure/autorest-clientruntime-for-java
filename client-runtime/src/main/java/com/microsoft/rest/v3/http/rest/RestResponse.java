// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.rest.v3.http.rest;

import com.microsoft.rest.v3.http.HttpHeaders;
import com.microsoft.rest.v3.http.HttpRequest;

public interface RestResponse<T> {

    /**
     * Get the HTTP response status code.
     *
     * @return the status code of the HTTP response.
     */
    int statusCode();

    /**
     * Get the headers from the HTTP response.
     *
     * @return an HttpHeaders instance containing the HTTP response headers.
     */
    HttpHeaders headers();

    /**
     * Get the HTTP request which resulted in this response.
     *
     * @return the HTTP request.
     */
    HttpRequest request();

    /**
     * @return the deserialized body of the HTTP response.
     */
    T body();
}
