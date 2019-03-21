/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.azure.common.credentials;

import com.azure.common.http.HttpRequest;
import reactor.core.publisher.Mono;

/**
 * Provides credentials to be put in the HTTP Authorization header.
 */
public interface AsyncServiceClientCredentials {
    /**
     * @param httpRequest The HTTP request that requires an authorization header.
     * @return The value containing currently valid credentials to put in the HTTP header, 'Authorization'.
     */
    Mono<String> authorizationHeaderValueAsync(HttpRequest httpRequest);
}
