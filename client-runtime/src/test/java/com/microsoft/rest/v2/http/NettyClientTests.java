package com.microsoft.rest.v2.http;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public class NettyClientTests {
    @Test
    public void testRequestBeforeShutdownSucceeds() throws Exception {
        final HttpClient.Factory factory = new NettyClient.Factory();
        HttpClient client = factory.create(null);
        HttpRequest request = new HttpRequest("", HttpMethod.GET, new URL("https://httpbin.org/get"), null);

        client.sendRequestAsync(request).blockingGet();
        factory.shutdown().blockingAwait();
    }

    @Test
    public void testRequestAfterShutdownIsRejected() throws Exception {
        final HttpClient.Factory factory = new NettyClient.Factory();
        HttpClient client = factory.create(null);
        HttpRequest request = new HttpRequest("", HttpMethod.GET, new URL("https://httpbin.org/get"), null);

        LoggerFactory.getLogger(getClass()).info("Closing factory");
        factory.shutdown().blockingAwait();

        try {
            LoggerFactory.getLogger(getClass()).info("Sending request");
            client.sendRequestAsync(request).blockingGet();
            fail();
        } catch (RejectedExecutionException ignored) {
            // expected
        }
    }

    @Test
    @Ignore("Passes inconsistently due to race condition")
    public void testInFlightRequestSucceedsAfterCancellation() throws Exception {
        // Retry a few times in case shutdown begins before the request is submitted to Netty
        for (int i = 0; i < 3; i++) {
            final HttpClient.Factory factory = new NettyClient.Factory();
            HttpClient client = factory.create(null);
            HttpRequest request = new HttpRequest("", HttpMethod.GET, new URL("https://httpbin.org/get"), null);

            Future<HttpResponse> asyncResponse = client.sendRequestAsync(request).toFuture();
            Thread.sleep(100);
            factory.shutdown().blockingAwait();

            boolean shouldRetry = false;
            try {
                asyncResponse.get(5, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                shouldRetry = true;
            }

            if (!shouldRetry) {
                break;
            }

            if (i == 2) {
                fail();
            } else {
                LoggerFactory.getLogger(getClass()).info("Shutdown started before sending request. Retry attempt " + i+1);
            }
        }
    }
}
