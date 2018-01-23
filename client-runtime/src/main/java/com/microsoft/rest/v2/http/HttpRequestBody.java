/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.http;

import io.reactivex.Flowable;

/**
 * The body of an HTTP request.
 */
public interface HttpRequestBody {
    /**
     * @return the MIME Content-Type of this request body.
     */
    String contentType();

    /**
     * @return A Flowable which provides this request body's content upon subscription.
     */
    Flowable<byte[]> content();
}
