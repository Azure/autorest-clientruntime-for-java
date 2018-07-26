/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure;

import com.microsoft.azure.serializer.AzureJacksonAdapter;
import com.microsoft.rest.RestClient;
import com.microsoft.rest.interceptors.RequestIdHeaderInterceptor;
import com.microsoft.rest.retry.RetryHandler;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class RequestIdHeaderInterceptorTests {
    private static final String REQUEST_ID_HEADER = "x-ms-client-request-id";

    @Test
    public void newRequestIdForEachCall() throws Exception {
        RestClient restClient = new RestClient.Builder()
                .withBaseUrl("http://localhost")
                .withSerializerAdapter(new AzureJacksonAdapter())
                .withResponseBuilderFactory(new AzureResponseBuilder.Factory())
                .withInterceptor(new RequestIdHeaderInterceptor())
                .withInterceptor(new Interceptor() {
                    private String firstRequestId = null;
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        if (request.header(REQUEST_ID_HEADER) != null) {
                            if (firstRequestId == null) {
                                firstRequestId = request.header(REQUEST_ID_HEADER);
                                return new Response.Builder()
                                        .code(200)
                                        .request(request)
                                        .message("OK")
                                        .protocol(Protocol.HTTP_1_1)
                                        .body(ResponseBody.create(MediaType.parse("text/plain"), "azure rocks"))
                                        .build();
                            } else if (!request.header(REQUEST_ID_HEADER).equals(firstRequestId)) {
                                return new Response.Builder()
                                        .code(200)
                                        .request(request)
                                        .message("OK")
                                        .protocol(Protocol.HTTP_1_1)
                                        .body(ResponseBody.create(MediaType.parse("text/plain"), "azure rocks"))
                                        .build();
                            }
                        }
                        return new Response.Builder().code(400).request(request)
                                .protocol(Protocol.HTTP_1_1).build();
                    }
                })
                .build();
        AzureServiceClient serviceClient = new AzureServiceClient(restClient) { };
        Response response = serviceClient.restClient().httpClient()
                .newCall(new Request.Builder().get().url("http://localhost").build()).execute();
        Assert.assertEquals(200, response.code());
        response = serviceClient.restClient().httpClient()
                .newCall(new Request.Builder().get().url("http://localhost").build()).execute();
        Assert.assertEquals(200, response.code());
    }

    @Test
    public void sameRequestIdForRetry() throws Exception {
        RestClient restClient = new RestClient.Builder()
                .withBaseUrl("http://localhost")
                .withSerializerAdapter(new AzureJacksonAdapter())
                .withResponseBuilderFactory(new AzureResponseBuilder.Factory())
                .withInterceptor(new RequestIdHeaderInterceptor())
                .withInterceptor(new RetryHandler())
                .withInterceptor(new Interceptor() {
                    private String firstRequestId = null;

                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        if (request.header(REQUEST_ID_HEADER) != null) {
                            if (firstRequestId == null) {
                                firstRequestId = request.header(REQUEST_ID_HEADER);
                                return new Response.Builder()
                                        .code(500)
                                        .request(request)
                                        .message("Error")
                                        .protocol(Protocol.HTTP_1_1)
                                        .body(ResponseBody.create(MediaType.parse("text/plain"), "azure rocks"))
                                        .build();
                            } else if (request.header(REQUEST_ID_HEADER).equals(firstRequestId)) {
                                return new Response.Builder()
                                        .code(200)
                                        .request(request)
                                        .message("OK")
                                        .protocol(Protocol.HTTP_1_1)
                                        .body(ResponseBody.create(MediaType.parse("text/plain"), "azure rocks"))
                                        .build();
                            }
                        }
                        return new Response.Builder().code(400).request(request)
                                .protocol(Protocol.HTTP_1_1).build();
                    }
                })
                .build();
        AzureServiceClient serviceClient = new AzureServiceClient(restClient) { };
        Response response = serviceClient.restClient().httpClient()
                .newCall(new Request.Builder().get().url("http://localhost").build()).execute();
        Assert.assertEquals(200, response.code());
    }
}
