/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.v2.credentials;

import io.reactivex.Single;

/**
 * Provides credentials to be put in the HTTP Authorization header.
 */
public interface AsyncServiceClientCredentials {
    /**
     * @param uri The URI to which the request is being made.
     * @return The value containing currently valid credentials to put in the HTTP header.
     */
    Single<String> authorizationHeaderValueAsync(String uri);
}
