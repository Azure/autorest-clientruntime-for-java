/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.http;

/**
 * The body of an HTTP request.
 */
public interface HttpRequestBody {
    /**
     * The length of this request body in bytes.
     * @return The length of this request body in bytes.
     */
    int contentLength();
}
