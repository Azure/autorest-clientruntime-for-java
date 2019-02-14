package com.microsoft.rest.v3.policy;

import com.microsoft.rest.v3.http.HttpClient;
import com.microsoft.rest.v3.http.HttpMethod;
import com.microsoft.rest.v3.http.HttpPipeline;
import com.microsoft.rest.v3.http.HttpPipelineBuilder;
import com.microsoft.rest.v3.http.HttpRequest;
import com.microsoft.rest.v3.http.HttpResponse;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public class HostPolicyTests {
    @Test
    public void withNoPort() throws MalformedURLException {
        final HttpPipeline pipeline = createPipeline("localhost", "ftp://localhost");
        pipeline.sendRequest(createHttpRequest("ftp://www.example.com")).block();
    }

    @Test
    public void withPort() throws MalformedURLException {
        final HttpPipeline pipeline = createPipeline("localhost", "ftp://localhost:1234");
        pipeline.sendRequest(createHttpRequest("ftp://www.example.com:1234"));
    }

    private static HttpPipeline createPipeline(String host, String expectedUrl) {
        return new HttpPipelineBuilder()
                .withPolicy(new HostPolicy(host))
                .withPolicy((context, next) -> {
                    assertEquals(expectedUrl, context.httpRequest().url().toString());
                    return next.process();
                })
                .withHttpClient(new HttpClient() {
                    @Override
                    public Mono<HttpResponse> sendRequestAsync(HttpRequest request) {
                        return Mono.empty(); // NOP
                    }
                })
            .build();
    }

    private static HttpRequest createHttpRequest(String url) throws MalformedURLException {
        return new HttpRequest("mock.caller", HttpMethod.GET, new URL(url), null);
    }
}
