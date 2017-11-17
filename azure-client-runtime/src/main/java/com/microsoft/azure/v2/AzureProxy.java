/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.v2;

import com.google.common.reflect.TypeToken;
import com.microsoft.azure.v2.annotations.AzureHost;
import com.microsoft.rest.v2.Pipeline;
import com.microsoft.rest.v2.protocol.SerializerAdapter;
import com.microsoft.rest.v2.InvalidReturnTypeException;
import com.microsoft.rest.v2.RestProxy;
import com.microsoft.rest.v2.SwaggerInterfaceParser;
import com.microsoft.rest.v2.SwaggerMethodParser;
import com.microsoft.rest.v2.http.HttpClient;
import com.microsoft.rest.v2.http.HttpRequest;
import com.microsoft.rest.v2.http.HttpResponse;
import rx.Observable;
import rx.Single;
import rx.exceptions.Exceptions;
import rx.functions.Func1;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

/**
 * This class can be used to create an Azure specific proxy implementation for a provided Swagger
 * generated interface.
 */
public final class AzureProxy extends RestProxy {
    private static long defaultPollingDelayInMilliseconds = 30 * 1000;

    /**
     * Create a new instance of RestProxy.
     * @param pipeline The Pipeline that will be used by this AzureProxy to send HttpRequests.
     * @param serializer The serializer that will be used to convert response bodies to POJOs.
     * @param interfaceParser The parser that contains information about the swagger interface that
     *                        this RestProxy "implements".
     */
    private AzureProxy(Pipeline pipeline, SerializerAdapter<?> serializer, SwaggerInterfaceParser interfaceParser) {
        super(pipeline, serializer, interfaceParser);
    }

    /**
     * @return The millisecond delay that will occur by default between long running operation polls.
     */
    public static long defaultDelayInMilliseconds() {
        return AzureProxy.defaultPollingDelayInMilliseconds;
    }

    /**
     * Set the millisecond delay that will occur by default between long running operation polls.
     * @param defaultPollingDelayInMilliseconds The number of milliseconds to delay before sending the next
     *                                   long running operation status poll.
     */
    public static void setDefaultPollingDelayInMilliseconds(long defaultPollingDelayInMilliseconds) {
        AzureProxy.defaultPollingDelayInMilliseconds = defaultPollingDelayInMilliseconds;
    }

    /**
     * Create a proxy implementation of the provided Swagger interface.
     * @param swaggerInterface The Swagger interface to provide a proxy implementation for.
     * @param azureEnvironment The azure environment that the proxy implementation will target.
     * @param pipeline The HTTP pipeline will be used to make REST calls.
     * @param serializer The serializer that will be used to convert POJOs to and from request and
     *                   response bodies.
     * @param <A> The type of the Swagger interface.
     * @return A proxy implementation of the provided Swagger interface.
     */
    @SuppressWarnings("unchecked")
    public static <A> A create(Class<A> swaggerInterface, AzureEnvironment azureEnvironment, Pipeline pipeline, SerializerAdapter<?> serializer) {
        String baseUrl = null;

        if (azureEnvironment != null) {
            final AzureHost azureHost = swaggerInterface.getAnnotation(AzureHost.class);
            if (azureHost != null) {
                baseUrl = azureEnvironment.url(azureHost.endpoint());
            }
        }

        return AzureProxy.create(swaggerInterface, baseUrl, pipeline, serializer);
    }

    /**
     * Create a proxy implementation of the provided Swagger interface.
     * @param swaggerInterface The Swagger interface to provide a proxy implementation for.
     * @param baseUrl The base URL (protocol and host) that the proxy implementation will target.
     * @param pipeline The internal HTTP pipeline that will be used to make REST calls.
     * @param serializer The serializer that will be used to convert POJOs to and from request and
     *                   response bodies.
     * @param <A> The type of the Swagger interface.
     * @return A proxy implementation of the provided Swagger interface.
     */
    @SuppressWarnings("unchecked")
    public static <A> A create(Class<A> swaggerInterface, String baseUrl, Pipeline pipeline, SerializerAdapter<?> serializer) {
        final SwaggerInterfaceParser interfaceParser = new SwaggerInterfaceParser(swaggerInterface, baseUrl);
        final AzureProxy azureProxy = new AzureProxy(pipeline, serializer, interfaceParser);
        return (A) Proxy.newProxyInstance(swaggerInterface.getClassLoader(), new Class[]{swaggerInterface}, azureProxy);
    }

    @Override
    protected Object handleAsyncHttpResponse(final HttpRequest httpRequest, Single<HttpResponse> asyncHttpResponse, final SwaggerMethodParser methodParser, Type returnType) {
        Object result;

        final TypeToken returnTypeToken = TypeToken.of(returnType);

        if (returnTypeToken.isSubtypeOf(Observable.class)) {
            final Type operationStatusType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
            final TypeToken operationStatusTypeToken = TypeToken.of(operationStatusType);
            if (!operationStatusTypeToken.isSubtypeOf(OperationStatus.class)) {
                throw new InvalidReturnTypeException("AzureProxy only supports swagger interface methods that return Observable (such as " + methodParser.fullyQualifiedMethodName() + "()) if the Observable's inner type that is OperationStatus (not " + returnType.toString() + ").");
            }
            else {
                final Type operationStatusResultType = ((ParameterizedType) operationStatusType).getActualTypeArguments()[0];
                result = createPollStrategy(httpRequest, asyncHttpResponse, methodParser)
                            .toObservable()
                            .flatMap(new Func1<PollStrategy, Observable<OperationStatus<Object>>>() {
                                @Override
                                public Observable<OperationStatus<Object>> call(final PollStrategy pollStrategy) {
                                    return pollStrategy.pollUntilDoneWithStatusUpdates(httpRequest, methodParser, operationStatusResultType);
                                }
                            });
            }
        }
        else {
            final Single<HttpResponse> lastAsyncHttpResponse = createPollStrategy(httpRequest, asyncHttpResponse, methodParser)
                    .flatMap(new Func1<PollStrategy, Single<HttpResponse>>() {
                        @Override
                        public Single<HttpResponse> call(PollStrategy pollStrategy) {
                            return pollStrategy.pollUntilDone();
                        }
                    });
            result = handleAsyncReturnType(httpRequest, lastAsyncHttpResponse, methodParser, returnType);
        }

        return result;
    }

    private Single<PollStrategy> createPollStrategy(final HttpRequest originalHttpRequest, final Single<HttpResponse> asyncOriginalHttpResponse, final SwaggerMethodParser methodParser) {
        return asyncOriginalHttpResponse
                .flatMap(new Func1<HttpResponse, Single<PollStrategy>>() {
                    @Override
                    public Single<PollStrategy> call(final HttpResponse originalHttpResponse) {
                        final int httpStatusCode = originalHttpResponse.statusCode();
                        final int[] longRunningOperationStatusCodes = new int[] {200, 201, 202};
                        return ensureExpectedStatus(originalHttpResponse, methodParser, longRunningOperationStatusCodes)
                                .flatMap(new Func1<HttpResponse, Single<? extends PollStrategy>>() {
                                    @Override
                                    public Single<? extends PollStrategy> call(HttpResponse response) {
                                        Single<PollStrategy> result = null;

                                        final Long parsedDelayInMilliseconds = PollStrategy.delayInMillisecondsFrom(originalHttpResponse);
                                        final long delayInMilliseconds = parsedDelayInMilliseconds != null ? parsedDelayInMilliseconds : AzureProxy.defaultDelayInMilliseconds();

                                        final String originalHttpRequestMethod = originalHttpRequest.httpMethod();

                                        PollStrategy pollStrategy = null;
                                        if (httpStatusCode == 200) {
                                            pollStrategy = AzureAsyncOperationPollStrategy.tryToCreate(AzureProxy.this, methodParser, originalHttpRequest, originalHttpResponse, delayInMilliseconds);
                                            if (pollStrategy != null) {
                                                result = Single.just(pollStrategy);
                                            }
                                            else {
                                                result = createProvisioningStateOrCompletedPollStrategy(originalHttpRequest, originalHttpResponse, methodParser, delayInMilliseconds);
                                            }
                                        }
                                        else if (originalHttpRequestMethod.equalsIgnoreCase("PUT") || originalHttpRequestMethod.equalsIgnoreCase("PATCH")) {
                                            if (httpStatusCode == 201) {
                                                pollStrategy = AzureAsyncOperationPollStrategy.tryToCreate(AzureProxy.this, methodParser, originalHttpRequest, originalHttpResponse, delayInMilliseconds);
                                                if (pollStrategy == null) {
                                                    result = createProvisioningStateOrCompletedPollStrategy(originalHttpRequest, originalHttpResponse, methodParser, delayInMilliseconds);
                                                }
                                            } else if (httpStatusCode == 202) {
                                                pollStrategy = AzureAsyncOperationPollStrategy.tryToCreate(AzureProxy.this, methodParser, originalHttpRequest, originalHttpResponse, delayInMilliseconds);
                                                if (pollStrategy == null) {
                                                    pollStrategy = LocationPollStrategy.tryToCreate(AzureProxy.this, methodParser, originalHttpRequest, originalHttpResponse, delayInMilliseconds);
                                                }
                                            }
                                        }
                                        else {
                                            if (httpStatusCode == 202) {
                                                pollStrategy = AzureAsyncOperationPollStrategy.tryToCreate(AzureProxy.this, methodParser, originalHttpRequest, originalHttpResponse, delayInMilliseconds);
                                                if (pollStrategy == null) {
                                                    pollStrategy = LocationPollStrategy.tryToCreate(AzureProxy.this, methodParser, originalHttpRequest, originalHttpResponse, delayInMilliseconds);
                                                    if (pollStrategy == null) {
                                                        throw new CloudException("Response does not contain an Azure-AsyncOperation or Location header.", originalHttpResponse);
                                                    }
                                                }
                                            }
                                        }

                                        if (pollStrategy == null && result == null) {
                                            pollStrategy = new CompletedPollStrategy(AzureProxy.this, methodParser, originalHttpResponse);
                                        }

                                        if (pollStrategy != null) {
                                            result = Single.just(pollStrategy);
                                        }

                                        return result;
                                    }
                                });
                    }
                });
    }

    private Single<PollStrategy> createProvisioningStateOrCompletedPollStrategy(final HttpRequest httpRequest, HttpResponse httpResponse, final SwaggerMethodParser methodParser, final long delayInMilliseconds) {
        Single<PollStrategy> result;

        final String httpRequestMethod = httpRequest.httpMethod();
        if (httpRequestMethod.equalsIgnoreCase("DELETE")
                || httpRequestMethod.equalsIgnoreCase("GET")
                || httpRequestMethod.equalsIgnoreCase("HEAD")
                || !methodParser.expectsResponseBody()) {
            result = Single.<PollStrategy>just(new CompletedPollStrategy(AzureProxy.this, methodParser, httpResponse));
        } else {
            final HttpResponse bufferedOriginalHttpResponse = httpResponse.buffer();
            result = bufferedOriginalHttpResponse.bodyAsStringAsync()
                    .map(new Func1<String, PollStrategy>() {
                        @Override
                        public PollStrategy call(String originalHttpResponseBody) {
                            if (originalHttpResponseBody == null || originalHttpResponseBody.isEmpty()) {
                                throw new CloudException("The HTTP response does not contain a body.", bufferedOriginalHttpResponse);
                            }

                            PollStrategy result;
                            try {
                                final SerializerAdapter<?> serializer = serializer();
                                final ResourceWithProvisioningState resource = serializer.deserialize(originalHttpResponseBody, ResourceWithProvisioningState.class, SerializerAdapter.Encoding.JSON);
                                if (resource != null && resource.properties() != null && !OperationState.isCompleted(resource.properties().provisioningState())) {
                                    result = new ProvisioningStatePollStrategy(AzureProxy.this, methodParser, httpRequest, resource.properties().provisioningState(), delayInMilliseconds);
                                } else {
                                    result = new CompletedPollStrategy(AzureProxy.this, methodParser, bufferedOriginalHttpResponse);
                                }
                            } catch (IOException e) {
                                throw Exceptions.propagate(e);
                            }

                            return result;
                        }
                    });
        }

        return result;
    }
}