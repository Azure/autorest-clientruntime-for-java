/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.azure.common;

import com.azure.common.configuration.ClientConfiguration;
import com.azure.common.credentials.ServiceClientCredentials;
import com.azure.common.http.HttpPipeline;
import com.azure.common.implementation.RestProxy;
import com.azure.common.implementation.serializer.SerializerAdapter;

/**
 * The base class for REST service clients.
 */
public abstract class ServiceClient {
    /**
     * The HTTP pipeline to send requests through.
     */
    private HttpPipeline httpPipeline;

    /**
     * The lazily-created serializer for this ServiceClient.
     */
    private SerializerAdapter serializerAdapter;

    /**
     * Creates ServiceClient.
     *
     * @param httpPipeline The HTTP pipeline to send requests through
     */
    protected ServiceClient(HttpPipeline httpPipeline) {
        this.httpPipeline = httpPipeline;
    }

    /**
     * @return the HTTP pipeline to send requests through.
     */
    public HttpPipeline httpPipeline() {
        return this.httpPipeline;
    }

    /**
     * @return the serializer for this ServiceClient.
     */
    public SerializerAdapter serializerAdapter() {
        if (this.serializerAdapter == null) {
            this.serializerAdapter = createSerializerAdapter();
        }
        return this.serializerAdapter;
    }

    /**
     * Gets a working {@link ClientConfiguration} with all of the default values.
     *
     * <b>Note</b>
     * If services require additional configuration to work by default.
     * They should override this and add additional configuration values.
     * @param credentials Credentials to authorize service with Azure.
     * @return A {@link ClientConfiguration} ServiceClients can use.
     */
    protected static ClientConfiguration getDefaultConfiguration(ServiceClientCredentials credentials) {
        return new ClientConfiguration().withCredentials(credentials);
    }

    protected SerializerAdapter createSerializerAdapter() {
        return RestProxy.createDefaultSerializer();
    }
}