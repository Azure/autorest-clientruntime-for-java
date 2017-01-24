/**
 *
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 *
 */

package com.microsoft.azure.credentials;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * Token credentials filter for placing a token credential into request headers.
 */
final class AzureTokenCredentialsInterceptor implements Interceptor {
    /**
     * The credentials instance to apply to the HTTP client pipeline.
     */
    private AzureTokenCredentials credentials;

    /**
     * Initialize a TokenCredentialsFilter class with a
     * TokenCredentials credential.
     *
     * @param credentials a TokenCredentials instance
     */
    public AzureTokenCredentialsInterceptor(AzureTokenCredentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String resource = credentials.environment().managementEndpoint();
        // Use graph resource if the host if graph endpoint
        if (credentials.environment().graphEndpoint().contains(chain.request().url().host())) {
            resource = credentials.environment().graphEndpoint();
        }
        Request newRequest = chain.request().newBuilder()
                .header("Authorization", "Bearer " + credentials.getToken(resource))
                .build();
        return chain.proceed(newRequest);
    }

    private Response sendRequestWithToken(Chain chain) throws IOException {
        String resource = credentials.environment().managementEndpoint();
        // Use graph resource if the host if graph endpoint
        if (chain.request().url().host().equals(credentials.environment().graphEndpoint())) {
            resource = credentials.environment().graphEndpoint();
        }
        Request newRequest = chain.request().newBuilder()
                .header("Authorization", "Bearer " + credentials.getToken(resource))
                .build();
        return chain.proceed(newRequest);
    }
}
