package com.microsoft.rest.v2;

import com.microsoft.rest.v2.http.HttpClient;
import com.microsoft.rest.v2.http.HttpRequest;
import com.microsoft.rest.v2.http.HttpResponse;
import com.microsoft.rest.v2.http.MockHttpResponse;
import com.microsoft.rest.v2.policy.RequestIdPolicy;
import org.junit.Test;
import rx.Single;

import static org.junit.Assert.*;

public class PipelineTests {
    @Test
    public void withNoRequestPolicies() {
        final String expectedHttpMethod = "GET";
        final String expectedUrl = "http://my.site.com";
        final Pipeline pipeline = Pipeline.build(new HttpClient() {
            @Override
            protected Single<HttpResponse> sendRequestInternalAsync(HttpRequest request) {
                assertEquals(0, request.headers().size());
                assertEquals(expectedHttpMethod, request.httpMethod());
                assertEquals(expectedUrl, request.url());
                return Single.<HttpResponse>just(new MockHttpResponse(200));
            }
        });

        final HttpResponse response = pipeline.sendRequestAsync(new HttpRequest("MOCK_CALLER_METHOD", expectedHttpMethod, expectedUrl)).toBlocking().value();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
    }

    @Test
    public void withUserAgentRequestPolicy() {
        final String expectedHttpMethod = "GET";
        final String expectedUrl = "http://my.site.com/1";
        final String expectedUserAgent = "my-user-agent";
        final HttpClient httpClient = new HttpClient() {
            @Override
            protected Single<HttpResponse> sendRequestInternalAsync(HttpRequest request) {
                assertEquals(1, request.headers().size());
                assertEquals(expectedUserAgent, request.headers().value("User-Agent"));
                assertEquals(expectedHttpMethod, request.httpMethod());
                assertEquals(expectedUrl, request.url());
                return Single.<HttpResponse>just(new MockHttpResponse(200));
            }
        };
        final Pipeline pipeline = new Pipeline.Builder(httpClient)
                .withUserAgent(expectedUserAgent)
                .build();
        final HttpResponse response = pipeline.sendRequestAsync(new HttpRequest("MOCK_CALLER_METHOD", expectedHttpMethod, expectedUrl)).toBlocking().value();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
    }

    @Test
    public void withRequestIdRequestPolicy() {
        final String expectedHttpMethod = "GET";
        final String expectedUrl = "http://my.site.com/1";
        final Pipeline pipeline = Pipeline.build(
                new HttpClient() {
                    @Override
                    protected Single<HttpResponse> sendRequestInternalAsync(HttpRequest request) {
                        assertEquals(1, request.headers().size());
                        final String requestId = request.headers().value("x-ms-client-request-id");
                        assertNotNull(requestId);
                        assertFalse(requestId.isEmpty());

                        assertEquals(expectedHttpMethod, request.httpMethod());
                        assertEquals(expectedUrl, request.url());
                        return Single.<HttpResponse>just(new MockHttpResponse(200));
                    }
                },
                new RequestIdPolicy.Factory());
        final HttpResponse response = pipeline.sendRequestAsync(new HttpRequest("MOCK_CALLER_METHOD", expectedHttpMethod, expectedUrl)).toBlocking().value();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
    }
}
