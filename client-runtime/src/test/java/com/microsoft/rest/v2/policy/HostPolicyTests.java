package com.microsoft.rest.v2.policy;

import com.microsoft.rest.v2.http.HttpRequest;
import com.microsoft.rest.v2.http.HttpResponse;
import org.junit.Test;
import rx.Single;

import static org.junit.Assert.assertEquals;

public class HostPolicyTests {
    @Test
    public void withNoProtocol() {
        final HostPolicy policy = createHostPolicy("localhost", "localhost");
        policy.sendAsync(createHttpRequest("www.bing.com"));
    }

    @Test
    public void withProtocol() {
        final HostPolicy policy = createHostPolicy("localhost", "ftp://localhost");
        policy.sendAsync(createHttpRequest("ftp://www.example.com"));
    }

    private static RequestPolicy createMockRequestPolicy(final String expectedUrl) {
        return new RequestPolicy() {
            @Override
            public Single<HttpResponse> sendAsync(HttpRequest request) {
                assertEquals(expectedUrl, request.url());
                return null;
            }
        };
    }

    private static HostPolicy createHostPolicy(String host, String expectedUrl) {
        return new HostPolicy.Factory(host).create(createMockRequestPolicy(expectedUrl), null);
    }

    private static HttpRequest createHttpRequest(String url) {
        return new HttpRequest("mock.caller", "GET", url);
    }
}
