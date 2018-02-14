/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.http;

import io.reactivex.Completable;
import io.reactivex.Single;

import java.net.Proxy;

/**
 * A generic interface for sending HTTP requests and getting responses.
 */
public abstract class HttpClient {
    /**
     * Send the provided request asynchronously, applying any request policies provided to the HttpClient instance.
     * @param request The HTTP request to send.
     * @return A {@link Single} representing the HTTP response that will arrive asynchronously.
     */
    public abstract Single<HttpResponse> sendRequestAsync(HttpRequest request);

    private static final class DefaultHttpClientHolder {
        // Putting this field in an inner class makes it so it is only instantiated when
        // one of the createDefault() methods instead of instantiating when any members are accessed.
        private static HttpClient.Factory defaultHttpClientFactory = new NettyClient.Factory();
    }

    /**
     * Create an instance of the default HttpClient type.
     * @return an instance of the default HttpClient type.
     */
    public static HttpClient createDefault() {
        return createDefault(null);
    }

    /**
     * Create an instance of the default HttpClient type with the provided configuration.
     * @param configuration The configuration to apply to the HttpClient.
     * @return an instance of the default HttpClient type.
     */
    public static HttpClient createDefault(HttpClient.Configuration configuration) {
        return DefaultHttpClientHolder.defaultHttpClientFactory.create(configuration);
    }

    /**
     * The set of parameters used to create an HTTP client.
     */
    public static final class Configuration {
        private final Proxy proxy;

        /**
         * @return The optional proxy to use.
         */
        public Proxy proxy() {
            return proxy;
        }

        /**
         * Creates a Configuration.
         * @param proxy The optional proxy to use.
         */
        public Configuration(Proxy proxy) {
            this.proxy = proxy;
        }
    }

    /**
     * Creates an HttpClient from a Configuration.
     */
    public interface Factory {
        /**
         * Creates an HttpClient with the given Configuration.
         * @param configuration the configuration.
         * @return the HttpClient.
         */
        HttpClient create(Configuration configuration);

        /**
         * Asynchronously awaits completion of in-flight tasks,
         * then closes shared resources associated with this HttpClient.Factory.
         * After this Completable completes, HttpClients created from this Factory can no longer be used.
         *
         * @return a Completable which shuts down the factory when subscribed to.
         */
        Completable shutdown();
    }
}
