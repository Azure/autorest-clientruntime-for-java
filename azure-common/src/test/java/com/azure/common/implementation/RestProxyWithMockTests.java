package com.azure.common.implementation;

import com.azure.common.annotations.BodyParam;
import com.azure.common.annotations.POST;
import com.azure.common.http.rest.Page;
import com.azure.common.http.rest.RestException;
import com.azure.common.annotations.ExpectedResponses;
import com.azure.common.annotations.GET;
import com.azure.common.annotations.HeaderCollection;
import com.azure.common.annotations.Host;
import com.azure.common.annotations.ReturnValueWireType;
import com.azure.common.entities.HttpBinJSON;
import com.azure.common.http.HttpClient;
import com.azure.common.http.HttpHeaders;
import com.azure.common.http.HttpPipeline;
import com.azure.common.http.HttpRequest;
import com.azure.common.http.HttpResponse;
import com.azure.common.http.MockHttpClient;
import com.azure.common.http.MockHttpResponse;
import com.azure.common.http.rest.RestPagedResponse;
import com.azure.common.http.rest.RestResponse;
import com.azure.common.http.rest.RestResponseBase;
import com.azure.common.http.ProxyOptions;
import com.azure.common.implementation.http.ContentType;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import wiremock.com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RestProxyWithMockTests extends RestProxyTests {
    @Override
    protected HttpClient createHttpClient() {
        return new MockHttpClient();
    }

    @Host("http://httpbin.org")
    private interface Service1 {
        @GET("Base64UrlBytes/10")
        @ReturnValueWireType(Base64Url.class)
        byte[] getBase64UrlBytes10();

        @GET("Base64UrlListOfBytes")
        @ReturnValueWireType(Base64Url.class)
        List<byte[]> getBase64UrlListOfBytes();

        @GET("Base64UrlListOfListOfBytes")
        @ReturnValueWireType(Base64Url.class)
        List<List<byte[]>> getBase64UrlListOfListOfBytes();

        @GET("Base64UrlMapOfBytes")
        @ReturnValueWireType(Base64Url.class)
        Map<String, byte[]> getBase64UrlMapOfBytes();

        @GET("DateTimeRfc1123")
        @ReturnValueWireType(DateTimeRfc1123.class)
        OffsetDateTime getDateTimeRfc1123();

        @GET("UnixTime")
        @ReturnValueWireType(UnixTime.class)
        OffsetDateTime getDateTimeUnix();
    }

    @Test
    public void service1GetBase64UrlBytes10() {
        final byte[] bytes = createService(Service1.class)
                .getBase64UrlBytes10();
        assertNotNull(bytes);
        assertEquals(10, bytes.length);
        for (int i = 0; i < 10; ++i) {
            assertEquals((byte) i, bytes[i]);
        }
    }

    @Test
    public void service1GetBase64UrlListOfBytes() {
        final List<byte[]> bytesList = createService(Service1.class)
                .getBase64UrlListOfBytes();
        assertNotNull(bytesList);
        assertEquals(3, bytesList.size());

        for (int i = 0; i < bytesList.size(); ++i) {
            final byte[] bytes = bytesList.get(i);
            assertNotNull(bytes);
            assertEquals((i + 1) * 10, bytes.length);
            for (int j = 0; j < bytes.length; ++j) {
                assertEquals((byte) j, bytes[j]);
            }
        }
    }

    @Test
    public void service1GetBase64UrlListOfListOfBytes() {
        final List<List<byte[]>> bytesList = createService(Service1.class)
                .getBase64UrlListOfListOfBytes();
        assertNotNull(bytesList);
        assertEquals(2, bytesList.size());

        for (int i = 0; i < bytesList.size(); ++i) {
            final List<byte[]> innerList = bytesList.get(i);
            assertEquals((i + 1) * 2, innerList.size());

            for (int j = 0; j < innerList.size(); ++j) {
                final byte[] bytes = innerList.get(j);
                assertNotNull(bytes);
                assertEquals((j + 1) * 5, bytes.length);
                for (int k = 0; k < bytes.length; ++k) {
                    assertEquals(k, bytes[k]);
                }
            }
        }
    }

    @Test
    public void service1GetBase64UrlMapOfBytes() {
        final Map<String, byte[]> bytesMap = createService(Service1.class)
                .getBase64UrlMapOfBytes();
        assertNotNull(bytesMap);
        assertEquals(2, bytesMap.size());

        for (int i = 0; i < bytesMap.size(); ++i) {
            final byte[] bytes = bytesMap.get(Integer.toString(i));

            final int expectedArrayLength = (i + 1) * 10;
            assertEquals(expectedArrayLength, bytes.length);
            for (int j = 0; j < expectedArrayLength; ++j) {
                assertEquals((byte) j, bytes[j]);
            }
        }
    }

    @Test
    public void service1GetDateTimeRfc1123() {
        final OffsetDateTime dateTime = createService(Service1.class)
                .getDateTimeRfc1123();
        assertNotNull(dateTime);
        assertEquals(OffsetDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC), dateTime);
    }

    @Test
    public void service1GetDateTimeUnix() {
        final OffsetDateTime dateTime = createService(Service1.class)
                .getDateTimeUnix();
        assertNotNull(dateTime);
        assertEquals(OffsetDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC), dateTime);
    }


    @Host("http://httpbin.org")
    interface ServiceErrorWithCharsetService {
        @GET("/get")
        @ExpectedResponses({400})
        HttpBinJSON get();
    }

    @Test
    public void ServiceErrorWithResponseContentType() {
        ServiceErrorWithCharsetService service = RestProxy.create(
                ServiceErrorWithCharsetService.class,
                new HttpPipeline(new SimpleMockHttpClient() {
                    @Override
                    public Mono<HttpResponse> send(HttpRequest request) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.set("Content-Type", "application/json");

                        HttpResponse response = new MockHttpResponse(request, 200, headers,
                                "{ \"error\": \"Something went wrong, but at least this JSON is valid.\"}".getBytes(StandardCharsets.UTF_8));
                        return Mono.just(response);
                    }
                }));

        try {
            service.get();
            fail();
        } catch (RuntimeException ex) {
            assertEquals(ex.getMessage(), "Status code 200, \"{ \"error\": \"Something went wrong, but at least this JSON is valid.\"}\"");
        }
    }

    @Test
    public void ServiceErrorWithResponseContentTypeBadJSON() {
        ServiceErrorWithCharsetService service = RestProxy.create(
                ServiceErrorWithCharsetService.class,
                new HttpPipeline(new SimpleMockHttpClient() {
                    @Override
                    public Mono<HttpResponse> send(HttpRequest request) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.set("Content-Type", "application/json");

                        HttpResponse response = new MockHttpResponse(request, 200, headers, "BAD JSON".getBytes(StandardCharsets.UTF_8));
                        return Mono.just(response);
                    }
                }));

        try {
            service.get();
            fail();
        } catch (RestException ex) {
            assertContains(ex.getMessage(), "Status code 200");
            assertContains(ex.getMessage(), "\"BAD JSON\"");
        }
    }

    @Test
    public void ServiceErrorWithResponseContentTypeCharset() {
        ServiceErrorWithCharsetService service = RestProxy.create(
                ServiceErrorWithCharsetService.class,
                new HttpPipeline(new SimpleMockHttpClient() {
                    @Override
                    public Mono<HttpResponse> send(HttpRequest request) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.set("Content-Type", "application/json; charset=UTF-8");

                        HttpResponse response = new MockHttpResponse(request, 200, headers,
                                "{ \"error\": \"Something went wrong, but at least this JSON is valid.\"}".getBytes(StandardCharsets.UTF_8));
                        return Mono.just(response);
                    }
                }));

        try {
            service.get();
            fail();
        } catch (RuntimeException ex) {
            assertEquals(ex.getMessage(), "Status code 200, \"{ \"error\": \"Something went wrong, but at least this JSON is valid.\"}\"");
        }
    }

    @Test
    public void ServiceErrorWithResponseContentTypeCharsetBadJSON() {
        ServiceErrorWithCharsetService service = RestProxy.create(
                ServiceErrorWithCharsetService.class,
                new HttpPipeline(new SimpleMockHttpClient() {
                    @Override
                    public Mono<HttpResponse> send(HttpRequest request) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.set("Content-Type", "application/json; charset=UTF-8");

                        HttpResponse response = new MockHttpResponse(request, 200, headers, "BAD JSON".getBytes(StandardCharsets.UTF_8));
                        return Mono.just(response);
                    }
                }));

        try {
            service.get();
            fail();
        } catch (RestException ex) {
            assertContains(ex.getMessage(), "Status code 200");
            assertContains(ex.getMessage(), "\"BAD JSON\"");
        }
    }

    static class KeyValue {
        @JsonProperty("key")
        private int key;

        @JsonProperty("value")
        private String value;

        KeyValue() { }
        KeyValue(int key, String value) {
            this.key = key;
            this.value = value;
        }

        int key() {
            return this.key;
        }

        String value() {
            return this.value;
        }
    }

    static class KeyValuePage implements Page<KeyValue> {
        @JsonProperty()
        private List<KeyValue> items;

        @JsonProperty("nextLink")
        private String nextLink;

        KeyValuePage() {
        }

        KeyValuePage(List<KeyValue> items, String nextLink) {
            this.items = items;
            this.nextLink = nextLink;
        }

        @Override
        public List<KeyValue> items() {
            return items;
        }

        @Override
        public String nextLink() {
            return nextLink;
        }
    }

    @Host("http://echo.org")
    interface Service26 {
        @POST("anything/json")
        @ExpectedResponses({200})
        RestPagedResponse<KeyValue> getPage(@BodyParam(ContentType.APPLICATION_JSON) Page<KeyValue> values);

        @POST("anything/json")
        @ExpectedResponses({200})
        Mono<RestPagedResponse<KeyValue>> getPageAsync(@BodyParam(ContentType.APPLICATION_JSON) Page<KeyValue> values);
    }

    @Test
    public void service26getPage() {
        List<KeyValue> array = new ArrayList<>();
        KeyValue key1 = new KeyValue(1, "Foo");
        KeyValue key2 = new KeyValue(2, "Bar");
        KeyValue key3 = new KeyValue(10, "Baz");

        array.add(key1);
        array.add(key2);
        array.add(key3);
        KeyValuePage page = new KeyValuePage(array, "SomeNextLink");

        RestPagedResponse<KeyValue> response = createService(Service26.class).getPage(page);
        assertNotNull(response);
        assertEquals(array.size(), response.body().size());
    }

    @Test
    public void service26getPageAsync() {
        List<KeyValue> array = new ArrayList<>();
        KeyValue key1 = new KeyValue(1, "Foo");
        KeyValue key2 = new KeyValue(2, "Bar");
        KeyValue key3 = new KeyValue(10, "Baz");

        array.add(key1);
        array.add(key2);
        array.add(key3);
        KeyValuePage page = new KeyValuePage(array, "SomeNextLink");

        StepVerifier.create(createService(Service26.class).getPageAsync(page))
                .assertNext(r -> {
                    assertEquals(r.items().size(), 3);
                    for (KeyValue keyValue : r.body()) {
                        assertTrue(array.removeIf(kv -> kv.key == keyValue.key && kv.value().equals(keyValue.value())));
                    }
                    assertTrue(array.isEmpty());
                })
                .verifyComplete();
    }

    private static class HeaderCollectionTypePublicFields {
        public String name;

        @HeaderCollection("header-collection-prefix-")
        public Map<String, String> headerCollection;
    }

    private static class HeaderCollectionTypeProtectedFields {
        protected String name;

        @HeaderCollection("header-collection-prefix-")
        protected Map<String, String> headerCollection;
    }

    private static class HeaderCollectionTypePrivateFields {
        private String name;

        @HeaderCollection("header-collection-prefix-")
        private Map<String, String> headerCollection;
    }

    private static class HeaderCollectionTypePackagePrivateFields {
        String name;

        @HeaderCollection("header-collection-prefix-")
        Map<String, String> headerCollection;
    }

    @Host("https://www.example.com")
    interface ServiceHeaderCollections {
        @GET("url/path")
        RestResponseBase<HeaderCollectionTypePublicFields, Void> publicFields();

        @GET("url/path")
        RestResponseBase<HeaderCollectionTypeProtectedFields, Void> protectedFields();

        @GET("url/path")
        RestResponseBase<HeaderCollectionTypePrivateFields, Void> privateFields();

        @GET("url/path")
        RestResponseBase<HeaderCollectionTypePackagePrivateFields, Void> packagePrivateFields();
    }

    private static final HttpClient headerCollectionHttpClient = new MockHttpClient() {
        @Override
        public Mono<HttpResponse> send(HttpRequest request) {
            final HttpHeaders headers = new HttpHeaders();
            headers.set("name", "Phillip");
            headers.set("header-collection-prefix-one", "1");
            headers.set("header-collection-prefix-two", "2");
            headers.set("header-collection-prefix-three", "3");
            final MockHttpResponse response = new MockHttpResponse(request, 200, headers);
            return Mono.<HttpResponse>just(response);
        }
    };

    private ServiceHeaderCollections createHeaderCollectionsService() {
        return createService(ServiceHeaderCollections.class, headerCollectionHttpClient);
    }

    private static void assertHeaderCollectionsRawHeaders(RestResponse<Void> response) {
        final HttpHeaders responseRawHeaders = response.headers();
        assertEquals("Phillip", responseRawHeaders.value("name"));
        assertEquals("1", responseRawHeaders.value("header-collection-prefix-one"));
        assertEquals("2", responseRawHeaders.value("header-collection-prefix-two"));
        assertEquals("3", responseRawHeaders.value("header-collection-prefix-three"));
        assertEquals(4, responseRawHeaders.size());
    }

    private static void assertHeaderCollections(Map<String, String> headerCollections) {
        final Map<String, String> expectedHeaderCollections = new HashMap<>();
        expectedHeaderCollections.put("one", "1");
        expectedHeaderCollections.put("two", "2");
        expectedHeaderCollections.put("three", "3");

        for (final String key : headerCollections.keySet()) {
            assertEquals(expectedHeaderCollections.get(key), headerCollections.get(key));
        }
        assertEquals(expectedHeaderCollections.size(), headerCollections.size());
    }

    @Test
    public void serviceHeaderCollectionPublicFields() {
        final RestResponseBase<HeaderCollectionTypePublicFields, Void> response = createHeaderCollectionsService()
                .publicFields();
        assertNotNull(response);
        assertHeaderCollectionsRawHeaders(response);

        final HeaderCollectionTypePublicFields responseHeaders = response.deserializedHeaders();
        assertNotNull(responseHeaders);
        assertEquals("Phillip", responseHeaders.name);
        assertHeaderCollections(responseHeaders.headerCollection);
    }

    @Test
    public void serviceHeaderCollectionProtectedFields() {
        final RestResponseBase<HeaderCollectionTypeProtectedFields, Void> response = createHeaderCollectionsService()
                .protectedFields();
        assertNotNull(response);
        assertHeaderCollectionsRawHeaders(response);

        final HeaderCollectionTypeProtectedFields responseHeaders = response.deserializedHeaders();
        assertNotNull(responseHeaders);
        assertEquals("Phillip", responseHeaders.name);
        assertHeaderCollections(responseHeaders.headerCollection);
    }

    @Test
    public void serviceHeaderCollectionPrivateFields() {
        final RestResponseBase<HeaderCollectionTypePrivateFields, Void> response = createHeaderCollectionsService()
                .privateFields();
        assertNotNull(response);
        assertHeaderCollectionsRawHeaders(response);

        final HeaderCollectionTypePrivateFields responseHeaders = response.deserializedHeaders();
        assertNotNull(responseHeaders);
        assertEquals("Phillip", responseHeaders.name);
        assertHeaderCollections(responseHeaders.headerCollection);
    }

    @Test
    public void serviceHeaderCollectionPackagePrivateFields() {
        final RestResponseBase<HeaderCollectionTypePackagePrivateFields, Void> response = createHeaderCollectionsService()
                .packagePrivateFields();
        assertNotNull(response);
        assertHeaderCollectionsRawHeaders(response);

        final HeaderCollectionTypePackagePrivateFields responseHeaders = response.deserializedHeaders();
        assertNotNull(responseHeaders);
        assertEquals("Phillip", responseHeaders.name);
        assertHeaderCollections(responseHeaders.headerCollection);
    }

    private static void assertContains(String value, String expectedSubstring) {
        assertTrue("Expected \"" + value + "\" to contain \"" + expectedSubstring + "\".", value.contains(expectedSubstring));
    }


    private static abstract class SimpleMockHttpClient implements HttpClient {

        @Override
        public abstract Mono<HttpResponse> send(HttpRequest request);

        @Override
        public HttpClient proxy(Supplier<ProxyOptions> proxyOptions) {
            throw new IllegalStateException("MockHttpClient.proxy not implemented.");
        }

        @Override
        public HttpClient wiretap(boolean enableWiretap) {
            throw new IllegalStateException("MockHttpClient.wiretap not implemented.");
        }

        @Override
        public HttpClient port(int port) {
            throw new IllegalStateException("MockHttpClient.port not implemented.");
        }
    }
}
