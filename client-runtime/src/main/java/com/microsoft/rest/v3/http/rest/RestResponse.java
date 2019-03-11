package com.microsoft.rest.v3.http.rest;

import com.microsoft.rest.v3.http.HttpHeaders;
import com.microsoft.rest.v3.http.HttpRequest;

public interface RestResponse<T> {

    /**
     * Get the response status code.
     *
     * @return the status code of the HTTP response
     */
    int statusCode();

    /**
     * Get the deserialized response headers.
     *
     * @return an object of type HttpHeaders containing the HTTP response headers.
     */
    HttpHeaders headers();

    /**
     * Get the request which resulted in this response.
     *
     * @return the request
     */
    HttpRequest request();

    /**
     * @return the deserialized body of the HTTP response
     */
    T body();
}
