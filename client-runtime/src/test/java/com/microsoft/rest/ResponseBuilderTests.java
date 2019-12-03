/**
 *
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 *
 */

package com.microsoft.rest;

import com.google.common.reflect.TypeToken;
import com.microsoft.rest.protocol.ResponseBuilder;
import com.microsoft.rest.protocol.SerializerAdapter;
import com.microsoft.rest.serializer.JacksonAdapter;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Assert;
import org.junit.Test;
import retrofit2.http.GET;
import retrofit2.http.Url;
import rx.Observable;

import java.io.IOException;

import static org.junit.Assert.fail;

public class ResponseBuilderTests {
    private interface Service {
        @GET
        Observable<retrofit2.Response<ResponseBody>> get(@Url String url);
    }

    @Test
    public void nullOnGet404() throws Exception {
        RestClient restClient = new RestClient.Builder()
                .withBaseUrl("https://microsoft.com/")
                .withSerializerAdapter(new JacksonAdapter())
                .withResponseBuilderFactory(new ServiceResponseBuilder.Factory())
                .withInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        return new Response.Builder()
                                .code(404)
                                .request(chain.request())
                                .protocol(Protocol.HTTP_1_1)
                                .message("not found")
                                .body(ResponseBody.create(MediaType.get("text/plain"), "{\"code\":\"NotFound\"}"))
                                .build();
                    }
                })
                .build();
        retrofit2.Response<ResponseBody> response = restClient.retrofit().create(Service.class).get("https://microsoft.com/").toBlocking().single();
        ServiceResponse<String> sr = restClient.responseBuilderFactory().<String, RestException>newInstance(restClient.serializerAdapter())
                .register(200, new TypeToken<String>() { }.getType())
                .registerError(RestException.class)
                .build(response);
        Assert.assertNull(sr.body());
        Assert.assertEquals(404, sr.response().code());
    }

    @Test
    public void throwOnGet404() {
        ResponseBuilder.Factory factory = new ResponseBuilder.Factory() {
            @Override
            public <T, E extends RestException> ResponseBuilder<T, E> newInstance(SerializerAdapter<?> serializerAdapter) {
                return new ServiceResponseBuilder.Factory().<T, E>newInstance(serializerAdapter)
                        .withThrowOnGet404(true);
            }
        };

        RestClient restClient = new RestClient.Builder()
                .withBaseUrl("https://microsoft.com/")
                .withSerializerAdapter(new JacksonAdapter())
                .withResponseBuilderFactory(factory)
                .withInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        return new Response.Builder()
                                .code(404)
                                .request(chain.request())
                                .protocol(Protocol.HTTP_1_1)
                                .message("not found")
                                .body(ResponseBody.create(MediaType.get("text/plain"), "{\"code\":\"NotFound\"}"))
                                .build();
                    }
                })
                .build();
        try {
            retrofit2.Response<ResponseBody> response = restClient.retrofit().create(Service.class).get("https://microsoft.com/").toBlocking().single();
            ServiceResponse<String> sr = restClient.responseBuilderFactory().<String, RestException>newInstance(restClient.serializerAdapter())
                    .register(200, new TypeToken<String>() { }.getType())
                    .registerError(RestException.class)
                    .build(response);
            fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof RestException);
            Assert.assertEquals("Status code 404, {\"code\":\"NotFound\"}", e.getMessage());
            Assert.assertEquals(404, ((RestException) e).response().code());
        }
    }
}
