/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;

import java.io.IOException;

/**
 * The body of an HTTP request.
 */
public abstract class HttpRequestBody {
    /**
     * The length of this request body in bytes.
     * @return The length of this request body in bytes.
     */
    public abstract long contentLength();

    /**
     * @return the MIME Content-Type of this request body.
     */
    public abstract String contentType();

    /**
     * @return A Flowable which provides this request body's content upon subscription.
     */
    public abstract Flowable<byte[]> content();

    /**
     * @return a Flowable which provides this request body's content in Netty ByteBufs upon subscription.
     */
    Flowable<ByteBuf> byteBufContent() {
        return content().map(new Function<byte[], ByteBuf>() {
            @Override
            public ByteBuf apply(byte[] bytes) throws Exception {
                return Unpooled.wrappedBuffer(bytes);
            }
        });
    }

    /**
     * Get a buffered version of this HttpRequestBody. If this HttpRequestBody
     * can only be read once, then calling this method will consume this
     * HttpRequestBody and the resulting object should be used instead.
     * @return A buffered version of this HttpRequestBody.
     * @throws IOException if there is a problem buffering.
     */
    public abstract HttpRequestBody buffer() throws IOException;
}
