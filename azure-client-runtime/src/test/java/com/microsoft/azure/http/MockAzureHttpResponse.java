/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.http;

import com.microsoft.rest.http.HttpHeaders;
import com.microsoft.rest.http.HttpResponse;
import com.microsoft.rest.protocol.SerializerAdapter;
import com.microsoft.rest.serializer.JacksonAdapter;
import rx.Single;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MockAzureHttpResponse extends HttpResponse {
    private final static SerializerAdapter<?> serializer = new JacksonAdapter();

    private final int statusCode;

    private final HttpHeaders headers;

    private final byte[] bodyBytes;

    public MockAzureHttpResponse(int statusCode, byte[] bodyBytes) {
        this.headers = new HttpHeaders();

        this.statusCode = statusCode;
        this.bodyBytes = bodyBytes;
    }

    public MockAzureHttpResponse(int statusCode) {
        this(statusCode, (byte[])null);
    }

    public MockAzureHttpResponse(int statusCode, String string) {
        this(statusCode, string == null ? null : string.getBytes());
    }

    public MockAzureHttpResponse(int statusCode, Object serializable) {
        this(statusCode, serialize(serializable));
    }

    private static byte[] serialize(Object serializable) {
        byte[] result = null;
        try {
            final String serializedString = serializer.serialize(serializable);
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
    public Single<? extends InputStream> bodyAsInputStreamAsync() {
        return Single.just(new ByteArrayInputStream(bodyBytes));
    }

    @Override
    public Single<byte[]> bodyAsByteArrayAsync() {
        return Single.just(bodyBytes);
    }

    @Override
    public Single<String> bodyAsStringAsync() {
        return Single.just(bodyBytes == null ? null : new String(bodyBytes));
    }

    public MockAzureHttpResponse withHeader(String headerName, String headerValue) {
        headers.set(headerName, headerValue);
        return this;
    }
}
