/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.v2.http;

import com.microsoft.rest.v2.http.HttpHeaders;
import com.microsoft.rest.v2.http.HttpResponse;
import com.microsoft.rest.v2.protocol.SerializerAdapter;
import com.microsoft.rest.v2.protocol.SerializerEncoding;
import com.microsoft.rest.v2.serializer.JacksonAdapter;
import io.reactivex.Flowable;
import io.reactivex.Single;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MockAzureHttpResponse extends HttpResponse {
    private final static SerializerAdapter<?> serializer = new JacksonAdapter();

    private final int statusCode;

    private final HttpHeaders headers;

    private final byte[] bodyBytes;

    public MockAzureHttpResponse(int statusCode, HttpHeaders headers, byte[] bodyBytes) {
        this.headers = headers;

        this.statusCode = statusCode;
        this.bodyBytes = bodyBytes;
    }

    public MockAzureHttpResponse(int statusCode, HttpHeaders headers) {
        this(statusCode, headers, new byte[0]);
    }

    public MockAzureHttpResponse(int statusCode, HttpHeaders headers, String string) {
        this(statusCode, headers, string == null ? new byte[0] : string.getBytes());
    }

    public MockAzureHttpResponse(int statusCode, HttpHeaders headers, Object serializable) {
        this(statusCode, headers, serialize(serializable));
    }

    private static byte[] serialize(Object serializable) {
        byte[] result = null;
        try {
            final String serializedString = serializer.serialize(serializable, SerializerEncoding.JSON);
            result = serializedString == null ? null : serializedString.getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public String headerValue(String headerName) {
        return headers.value(headerName);
    }

    @Override
    public HttpHeaders headers() {
        return new HttpHeaders(headers);
    }

    @Override
    public Single<byte[]> bodyAsByteArrayAsync() {
        return Single.just(bodyBytes);
    }

    @Override
    public Flowable<byte[]> streamBodyAsync() {
        return Flowable.just(bodyBytes);
    }

    @Override
    public Single<String> bodyAsStringAsync() {
        return Single.just(new String(bodyBytes));
    }

    public MockAzureHttpResponse withHeader(String headerName, String headerValue) {
        headers.set(headerName, headerValue);
        return this;
    }
}
