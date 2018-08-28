/**
 *
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 *
 */

package com.microsoft.rest.v2;

import com.microsoft.rest.v2.annotations.BodyParam;
import com.microsoft.rest.v2.annotations.GET;
import com.microsoft.rest.v2.annotations.Host;
import com.microsoft.rest.v2.annotations.PUT;
import com.microsoft.rest.v2.entities.AccessPolicy;
import com.microsoft.rest.v2.entities.SignedIdentifierInner;
import com.microsoft.rest.v2.entities.SignedIdentifiersWrapper;
import com.microsoft.rest.v2.entities.Slideshow;
import com.microsoft.rest.v2.http.HttpClient;
import com.microsoft.rest.v2.http.HttpHeaders;
import com.microsoft.rest.v2.http.HttpMethod;
import com.microsoft.rest.v2.http.HttpPipeline;
import com.microsoft.rest.v2.http.HttpRequest;
import com.microsoft.rest.v2.http.HttpResponse;
import com.microsoft.rest.v2.http.MockHttpResponse;
import com.microsoft.rest.v2.policy.DecodingPolicyFactory;
import com.microsoft.rest.v2.protocol.SerializerEncoding;
import com.microsoft.rest.v2.serializer.JacksonAdapter;
import com.microsoft.rest.v2.util.FlowableUtil;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import org.junit.Test;
import wiremock.com.google.common.reflect.TypeToken;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;


public class RestProxyXMLTests {
    static class MockXMLHTTPClient extends HttpClient {
        private HttpResponse response(String resource) throws IOException, URISyntaxException {
            URL url = getClass().getClassLoader().getResource(resource);
            byte[] bytes = Files.readAllBytes(Paths.get(url.toURI()));
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/xml");
            HttpResponse res = new MockHttpResponse(200, headers, bytes);
            return res;
        }

        @Override
        public Single<HttpResponse> sendRequestAsync(HttpRequest request) {
            try {
                if (request.url().toString().endsWith("GetContainerACLs")) {
                    return Single.just(response("GetContainerACLs.xml"));
                } else if (request.url().toString().endsWith("GetXMLWithAttributes")) {
                    return Single.just(response("GetXMLWithAttributes.xml"));
                } else {
                    return Single.<HttpResponse>just(new MockHttpResponse(404));
                }
            } catch (IOException | URISyntaxException e) {
                return Single.error(e);
            }
        }
    }

    @Host("http://unused")
    interface MyXMLService {
        @GET("GetContainerACLs")
        SignedIdentifiersWrapper getContainerACLs();

        @PUT("SetContainerACLs")
        void setContainerACLs(@BodyParam("application/xml") SignedIdentifiersWrapper signedIdentifiers);
    }

    @Test
    public void canReadXMLResponse() throws Exception {
        MyXMLService myXMLService = RestProxy.create(MyXMLService.class, HttpPipeline.build(new MockXMLHTTPClient(), new DecodingPolicyFactory()), new JacksonAdapter());
        List<SignedIdentifierInner> identifiers = myXMLService.getContainerACLs().signedIdentifiers();
        assertNotNull(identifiers);
        assertNotEquals(0, identifiers.size());
    }

    static class MockXMLReceiverClient extends HttpClient {
        byte[] receivedBytes = null;

        @Override
        public Single<HttpResponse> sendRequestAsync(HttpRequest request) {
            if (request.url().toString().endsWith("SetContainerACLs")) {
                return FlowableUtil.collectBytesInArray(request.body())
                        .map(new Function<byte[], HttpResponse>() {
                            @Override
                            public HttpResponse apply(byte[] bytes) throws Exception {
                                receivedBytes = bytes;
                                return new MockHttpResponse(200);
                            }
                        });
            } else {
                return Single.<HttpResponse>just(new MockHttpResponse(404));
            }
        }
    }

    @Test
    public void canWriteXMLRequest() throws Exception {
        URL url = getClass().getClassLoader().getResource("GetContainerACLs.xml");
        byte[] bytes = Files.readAllBytes(Paths.get(url.toURI()));
        HttpRequest request = new HttpRequest("canWriteXMLRequest", HttpMethod.PUT, new URL("http://unused/SetContainerACLs"), null);
        request.withBody(bytes);

        SignedIdentifierInner si = new SignedIdentifierInner();
        si.withId("MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=");

        AccessPolicy ap = new AccessPolicy();
        ap.withStart(OffsetDateTime.parse("2009-09-28T08:49:37.0000000Z"));
        ap.withExpiry(OffsetDateTime.parse("2009-09-29T08:49:37.0000000Z"));
        ap.withPermission("rwd");

        si.withAccessPolicy(ap);
        List<SignedIdentifierInner> expectedAcls = Collections.singletonList(si);

        JacksonAdapter serializer = new JacksonAdapter();
        MockXMLReceiverClient httpClient = new MockXMLReceiverClient();
        MyXMLService myXMLService = RestProxy.create(MyXMLService.class, HttpPipeline.build(httpClient, new DecodingPolicyFactory()), serializer);
        SignedIdentifiersWrapper wrapper = new SignedIdentifiersWrapper(expectedAcls);
        myXMLService.setContainerACLs(wrapper);

        SignedIdentifiersWrapper actualAclsWrapped = serializer.deserialize(
                new String(httpClient.receivedBytes, StandardCharsets.UTF_8),
                new TypeToken<SignedIdentifiersWrapper>() {}.getType(),
                SerializerEncoding.XML);

        List<SignedIdentifierInner> actualAcls = actualAclsWrapped.signedIdentifiers();

        // Ideally we'd just check for "things that matter" about the XML-- e.g. the tag names, structure, and attributes needs to be the same,
        // but it doesn't matter if one document has a trailing newline or has UTF-8 in the header instead of utf-8, or if comments are missing.
        assertEquals(expectedAcls.size(), actualAcls.size());
        assertEquals(expectedAcls.get(0).id(), actualAcls.get(0).id());
        assertEquals(expectedAcls.get(0).accessPolicy().expiry(), actualAcls.get(0).accessPolicy().expiry());
        assertEquals(expectedAcls.get(0).accessPolicy().start(), actualAcls.get(0).accessPolicy().start());
        assertEquals(expectedAcls.get(0).accessPolicy().permission(), actualAcls.get(0).accessPolicy().permission());
    }

    @Host("http://unused")
    public interface MyXMLServiceWithAttributes {
        @GET("GetXMLWithAttributes")
        Slideshow getSlideshow();
    }

    @Test
    public void canDeserializeXMLWithAttributes() throws Exception {
        JacksonAdapter serializer = new JacksonAdapter();
        MyXMLServiceWithAttributes myXMLService = RestProxy.create(
                MyXMLServiceWithAttributes.class,
                HttpPipeline.build(new MockXMLHTTPClient(), new DecodingPolicyFactory()),
                serializer);

        Slideshow slideshow = myXMLService.getSlideshow();
        assertEquals("Sample Slide Show", slideshow.title);
        assertEquals("Date of publication", slideshow.date);
        assertEquals("Yours Truly", slideshow.author);
        assertEquals(2, slideshow.slides.length);

        assertEquals("all", slideshow.slides[0].type);
        assertEquals("Wake up to WonderWidgets!", slideshow.slides[0].title);
        assertNull(slideshow.slides[0].items);

        assertEquals("all", slideshow.slides[1].type);
        assertEquals("Overview", slideshow.slides[1].title);
        assertEquals(3, slideshow.slides[1].items.length);
        assertEquals("Why WonderWidgets are great", slideshow.slides[1].items[0]);
        assertEquals("", slideshow.slides[1].items[1]);
        assertEquals("Who buys WonderWidgets", slideshow.slides[1].items[2]);

        String xml = serializer.serialize(slideshow, SerializerEncoding.XML);
        Slideshow newSlideshow = serializer.deserialize(xml, Slideshow.class, SerializerEncoding.XML);
        String newXML = serializer.serialize(newSlideshow, SerializerEncoding.XML);
        assertEquals(xml, newXML);
    }
}
