package com.azure.common.http.rest;

import com.azure.common.http.HttpHeaders;
import com.azure.common.http.HttpRequest;

import java.util.List;

public class RestPagedResponseBase<H, T> implements RestPagedResponse<T> {
    private final HttpRequest request;
    private final int statusCode;
    private final H deserializedHeaders;
    private final HttpHeaders headers;
    private final List<T> items;
    private final String nextLink;

    public RestPagedResponseBase(HttpRequest request, int statusCode, HttpHeaders headers, Page<T> page, H deserializedHeaders) {
        this.request = request;
        this.statusCode = statusCode;
        this.headers = headers;
        this.items = page.items();
        this.nextLink = page.nextLink();
        this.deserializedHeaders = deserializedHeaders;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> items() { return items; }

    /**
     * {@inheritDoc}
     */
    @Override
    public String nextLink() { return nextLink; }

    /**
     * {@inheritDoc}
     */
    @Override
    public int statusCode() { return statusCode; }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpHeaders headers() { return headers; }

    /**
     * @return the request which resulted in this PagedRequestResponse.
     */
    @Override
    public HttpRequest request() { return request; }

    /**
     * Get the headers from the HTTP response, transformed into the header type H.
     *
     * @return an instance of header type H, containing the HTTP response headers.
     */
    public H deserializedHeaders() {
        return deserializedHeaders;
    }

    @Override
    public void close() {

    }
}
