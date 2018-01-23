/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.http;

import io.reactivex.Flowable;

/**
 * An HTTP request body which is given by subscribing to a Flowable.
 * The Flowable must support multiple subscription.
 */
public final class FlowableHttpRequestBody implements HttpRequestBody {
    private final String contentType;
    private Flowable<byte[]> content;

    /**
     * Create a new FlowableHttpRequestBody.
     * @param contentType the MIME type of the content
     * @param content the flowable content. Must support multiple subscription.
     */
    public FlowableHttpRequestBody(String contentType, Flowable<byte[]> content) {
        this.contentType = contentType;
        this.content = content;
    }

    @Override
    public String contentType() {
        return contentType;
    }

    @Override
    public Flowable<byte[]> content() {
        return content;
    }
}
