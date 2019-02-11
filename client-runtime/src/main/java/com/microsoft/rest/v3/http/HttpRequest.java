/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v3.http;

import com.microsoft.rest.v3.Context;
import com.microsoft.rest.v3.protocol.HttpResponseDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;

import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * This class contains all of the details necessary for sending a HTTP request through a HttpClient.
 */
public class HttpRequest {
    private String callerMethod;
    private HttpMethod httpMethod;
    private URL url;
    private HttpHeaders headers;
    private Flux<ByteBuf> body;
    private Context context;
    private final HttpResponseDecoder responseDecoder;

    /**
     * Create a new HttpRequest object with the provided HTTP method (GET, POST, PUT, etc.) and the
     * provided URL.
     * @param callerMethod The fully qualified method that was called to invoke this HTTP request.
     * @param httpMethod The HTTP method to use with this request.
     * @param url The URL where this HTTP request should be sent to.
     * @param responseDecoder the which decodes messages sent in response to this HttpRequest.
     */
    public HttpRequest(String callerMethod, HttpMethod httpMethod, URL url, HttpResponseDecoder responseDecoder) {
        this.callerMethod = callerMethod;
        this.httpMethod = httpMethod;
        this.url = url;
        this.headers = new HttpHeaders();
        this.responseDecoder = responseDecoder;
    }

    /**
     * Create a new HttpRequest object.
     * @param callerMethod The fully qualified method that was called to invoke this HTTP request.
     * @param httpMethod The HTTP method to use with this request.
     * @param url The URL where this HTTP request should be sent to.
     * @param headers The HTTP headers to use with this request.
     * @param body The body of this HTTP request.
     * @param responseDecoder the which decodes messages sent in response to this HttpRequest.
     */
    public HttpRequest(String callerMethod, HttpMethod httpMethod, URL url, HttpHeaders headers, Flux<ByteBuf> body, HttpResponseDecoder responseDecoder) {
        this.callerMethod = callerMethod;
        this.httpMethod = httpMethod;
        this.url = url;
        this.headers = headers;
        this.body = body;
        this.responseDecoder = responseDecoder;
    }

    /**
     * Get the fully qualified method that was called to invoke this HTTP request.
     * @return The fully qualified method that was called to invoke this HTTP request.
     */
    public String callerMethod() {
        return callerMethod;
    }

    /**
     * Set the caller method for this request.
     * @param callerMethod The fully qualified method that was called to invoke this HTTP request.
     * @return This HttpRequest instance for chaining.
     */
    public HttpRequest withCallerMethod(String callerMethod) {
        this.callerMethod = callerMethod;
        return this;
    }

    /**
     * Get the HTTP method that this request will use.
     * @return The HTTP method that this request will use.
     */
    public HttpMethod httpMethod() {
        return httpMethod;
    }

    /**
     * Set the HTTP method that this request will use.
     * @param httpMethod The HTTP method to use, e.g. "GET".
     * @return This HttpRequest so that multiple operations can be chained together.
     */
    public HttpRequest withHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    /**
     * Get the URL that this request will be sent to.
     * @return The URL that this request will be sent to.
     */
    public URL url() {
        return url;
    }

    /**
     * Set the URL that this request will be sent to.
     * @param url The new URL that this request will be sent to.
     * @return This HttpRequest so that multiple operations can be chained together.
     */
    public HttpRequest withUrl(URL url) {
        this.url = url;
        return this;
    }

    /**
     * Get the {@link HttpResponseDecoder} which decodes messages sent in response to this HttpRequest.
     * @return the response decoder
     */
    public HttpResponseDecoder responseDecoder() {
        return responseDecoder;
    }

    /**
     * Get the headers for this request.
     * @return The headers for this request.
     */
    public HttpHeaders headers() {
        return headers;
    }

    /**
     * Set the headers for this request.
     * @param headers The set of headers to send for this request.
     * @return This HttpRequest so that multiple operations can be chained together.
     */
    public HttpRequest withHeaders(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }

    /**
     * Add the provided headerName and headerValue to the list of headers for this request.
     * @param headerName The name of the header.
     * @param headerValue The value of the header.
     * @return This HttpRequest so that multiple operations can be chained together.
     */
    public HttpRequest withHeader(String headerName, String headerValue) {
        headers.set(headerName, headerValue);
        return this;
    }

    /**
     * Get the body for this HttpRequest.
     * @return The body for this HttpRequest.
     */
    public Flux<ByteBuf> body() {
        return body;
    }

    /**
     * Set the body of this HTTP request.
     * @param body The body of this HTTP request.
     * @return This HttpRequest so that multiple operations can be chained together.
     */
    public HttpRequest withBody(String body) {
        final byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        return withBody(bodyBytes);
    }

    /**
     * Set the body of this HTTP request, automatically setting the Content-Length header based on the given body's length.
     *
     * @param body The body of this HTTP request.
     * @return This HttpRequest so that multiple operations can be chained together.
     */
    public HttpRequest withBody(byte[] body) {
        headers.set("Content-Length", String.valueOf(body.length));
        // Unpooled.wrappedBuffer(body) ByteBuf in unpooled heap
        return withBody(Flux.just(Unpooled.wrappedBuffer(body)));
    }

    /**
     * Set the body of this HTTP request, leaving request headers unmodified.
     * Users must set the Content-Length header to indicate the length of the new body, or use Transfer-Encoding: chunked.
     *
     * @param body The body of this HTTP request.
     * @return This HttpRequest so that multiple operations can be chained together.
     */
    public HttpRequest withBody(Flux<ByteBuf> body) {
        this.body = body;
        return this;
    }

    /**
     * @return the {@link Context} associated with this HttpRequest
     */
    public Context context() {
        return context;
    }

    /**
     * @param context the context to associate with this HttpRequest
     * @return This HttpRequest so that multiple operations can be chained together.
     */
    public HttpRequest withContext(Context context) {
        this.context = context;
        return this;
    }

    /**
     * Performs a deep clone of this HTTP request. The main purpose of this is so that this
     * HttpRequest can be changed and the resulting HttpRequest can be a backup. This means
     * that the buffered HttpHeaders and body must not be able to change from side effects of
     * this HttpRequest.
     * @return A new HTTP request instance with cloned instances of all mutable properties.
     */
    public HttpRequest buffer() {
        final HttpHeaders bufferedHeaders = new HttpHeaders(headers);
        return new HttpRequest(callerMethod, httpMethod, url, bufferedHeaders, body, responseDecoder);
    }
}
