/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.http;

/**
 * A HTTP request body that contains a byte[].
 */
public class ByteArrayRequestBody implements HttpRequestBody {
    private final byte[] contents;

    /**
     * Create a new ByteArrayHttpRequestBody with the provided byte[].
     * @param contents The byte[] to store in this ByteArrayHttpRequestBody.
     */
    public ByteArrayRequestBody(byte[] contents) {
        this.contents = contents;
    }

    @Override
    public int contentLength() {
        return contents.length;
    }

    public byte[] content() {
        return contents;
    }
}
