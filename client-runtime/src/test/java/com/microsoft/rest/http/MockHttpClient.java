/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.http;

import com.microsoft.rest.HttpBinJSON;
import com.microsoft.rest.policy.RequestPolicy;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import rx.Single;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This HttpClient attempts to mimic the behavior of http://httpbin.org without ever making a network call.
 */
public class MockHttpClient extends HttpClient {
    public MockHttpClient() {}

    public MockHttpClient(List<RequestPolicy.Factory> policyFactories) {
        super(policyFactories);
    }

    @Override
    public Single<HttpResponse> sendRequestInternalAsync(HttpRequest request) {
        HttpResponse response = null;

        try {
            final URI requestUrl = new URI(request.url());
            final String requestHost = requestUrl.getHost();
            if (requestHost.equalsIgnoreCase("httpbin.org")) {
                final String requestPath = requestUrl.getPath();
                final String requestPathLower = requestPath.toLowerCase();
                if (requestPathLower.equals("/anything") || requestPathLower.startsWith("/anything/")) {
                    final HttpBinJSON json = new HttpBinJSON();
                    json.url = request.url()
                            // This is just to mimic the behavior we've seen with httpbin.org.
                            .replace("%20", " ");
                    json.headers = toMap(request.headers());
                    response = new MockHttpResponse(200, json);
                }
                else if (requestPathLower.startsWith("/bytes/")) {
                    final String byteCountString = requestPath.substring("/bytes/".length());
                    final int byteCount = Integer.parseInt(byteCountString);
                    response = new MockHttpResponse(200, new byte[byteCount]);
                }
                else if (requestPathLower.equals("/delete")) {
                    final HttpBinJSON json = new HttpBinJSON();
                    json.data = bodyToString(request);
                    response = new MockHttpResponse(200, json);
                }
                else if (requestPathLower.equals("/get")) {
                    final HttpBinJSON json = new HttpBinJSON();
                    json.url = request.url();
                    json.headers = toMap(request.headers());
                    response = new MockHttpResponse(200, json);
                }
                else if (requestPathLower.equals("/patch")) {
                    final HttpBinJSON json = new HttpBinJSON();
                    json.data = bodyToString(request);
                    response = new MockHttpResponse(200, json);
                }
                else if (requestPathLower.equals("/post")) {
                    final HttpBinJSON json = new HttpBinJSON();
                    json.data = bodyToString(request);
                    response = new MockHttpResponse(200, json);
                }
                else if (requestPathLower.equals("/put")) {
                    final HttpBinJSON json = new HttpBinJSON();
                    json.data = bodyToString(request);
                    response = new MockHttpResponse(200, json);
                }
            }
        }
        catch (Exception ignored) {
        }

        return Single.just(response);
    }

    private static String bodyToString(HttpRequest request) throws IOException {
        if (request.body() instanceof ByteArrayRequestBody) {
            return new String(((ByteArrayRequestBody) request.body()).content());
        } else if (request.body() instanceof FileRequestBody) {
            FileSegment segment = ((FileRequestBody) request.body()).content();
            ByteBuf requestContent = ByteBufAllocator.DEFAULT.buffer(segment.length());
            requestContent.writeBytes(segment.fileChannel(), segment.offset(), segment.length());
            return requestContent.toString(Charset.defaultCharset());
        } else {
            throw new IllegalArgumentException("Only ByteArrayRequestBody or FileRequestBody are supported");
        }
    }

    private static Map<String, String> toMap(HttpHeaders headers) {
        final Map<String, String> result = new HashMap<>();
        for (final HttpHeader header : headers) {
            result.put(header.name(), header.value());
        }
        return result;
    }
}
