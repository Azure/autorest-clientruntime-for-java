/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2;

import com.microsoft.rest.v2.http.HttpClient;
import com.microsoft.rest.v2.http.HttpPipeline;
import com.microsoft.rest.v2.http.HttpRequest;
import com.microsoft.rest.v2.http.HttpResponse;
import com.microsoft.rest.v2.http.MockHttpClient;
import com.microsoft.rest.v2.http.MockHttpResponse;
import com.microsoft.rest.v2.policy.UserAgentPolicy;
import org.junit.Assert;
import org.junit.Test;

import rx.Single;

import java.util.Collections;

public class UserAgentTests {
    @Test
    public void defaultUserAgentTests() throws Exception {
        HttpPipeline pipeline = HttpPipeline.build(
            new MockHttpClient() {
                @Override
                public Single<HttpResponse> sendRequestAsync(HttpRequest request) {
                    Assert.assertEquals(
                            request.headers().value("User-Agent"),
                            "AutoRest-Java");
                    return Single.<HttpResponse>just(new MockHttpResponse(200));
                }
            },
            new UserAgentPolicy.Factory("AutoRest-Java"));

        HttpResponse response = pipeline.sendRequestAsync(new HttpRequest(
                "defaultUserAgentTests",
                "GET", "http://localhost")).toBlocking().value();

        Assert.assertEquals(200, response.statusCode());
    }

    @Test
    public void customUserAgentTests() throws Exception {
        HttpPipeline pipeline = HttpPipeline.build(
            new MockHttpClient() {
                @Override
                public Single<HttpResponse> sendRequestAsync(HttpRequest request) {
                    String header = request.headers().value("User-Agent");
                    Assert.assertEquals("Awesome", header);
                    return Single.<HttpResponse>just(new MockHttpResponse(200));
                }
            },
            new UserAgentPolicy.Factory("Awesome"));

        HttpResponse response = pipeline.sendRequestAsync(new HttpRequest("customUserAgentTests", "GET", "http://localhost")).toBlocking().value();
        Assert.assertEquals(200, response.statusCode());
    }
}
