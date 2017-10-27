/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest;


import com.microsoft.rest.annotations.ExpectedResponses;
import com.microsoft.rest.annotations.GET;
import com.microsoft.rest.annotations.Host;
import com.microsoft.rest.entities.HttpBinJSON;
import com.microsoft.rest.http.HttpClient;
import com.microsoft.rest.http.HttpClient.Configuration;
import com.microsoft.rest.http.NettyClient;
import com.microsoft.rest.policy.RequestPolicy.Factory;
import com.microsoft.rest.serializer.JacksonAdapter;
import org.junit.Assert;
import org.junit.Test;
import rx.Single;
import rx.Subscription;
import rx.functions.Action1;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

public class CancellationTests {

    private HttpClient httpClient;
    private Service proxy;

    public CancellationTests() {
        httpClient = new NettyClient.Factory(1, 1).create(new Configuration(new ArrayList<Factory>(), null));
        proxy = RestProxy.create(Service.class, null, httpClient, new JacksonAdapter());
    }

    @Host("http://httpbin.org")
    private interface Service {
        @GET("bytes/10737418240")
        @ExpectedResponses({200})
        Single<byte[]> getByteArrayAsync();

        @GET("stream-bytes/10737418240")
        @ExpectedResponses({200})
        Single<InputStream> getByteArrayAsStreamAsync();

        @GET("anything")
        @ExpectedResponses({200})
        HttpBinJSON getAnything();
    }

    @Test
    public void cancelOperationInProgress() throws Exception {
        Subscription subscription = proxy.getByteArrayAsync().subscribe(new Action1<byte[]>() {
            @Override
            public void call(byte[] bytes) {
                fail();
            }
        });

        Thread.sleep(1000);

        subscription.unsubscribe();
        Assert.assertTrue(subscription.isUnsubscribed());
    }

    @Test
    public void cancelStreamingInProgress() throws Exception {
        final AtomicInteger received = new AtomicInteger();
        Subscription subscription = proxy.getByteArrayAsStreamAsync()
                .subscribe(new Action1<InputStream>() {
            @Override
            public void call(InputStream inputStream) {
                try {
                    while (inputStream.read() >= 0) {
                        received.incrementAndGet();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread.sleep(3000);

//        subscription.unsubscribe();
//        Assert.assertTrue(subscription.isUnsubscribed());
        System.out.println("received " + received.get());

        Thread.sleep(5000);
        System.out.println("received " + received.get());
    }

    @Test
    public void cancelOperationBeforeSent() throws Exception {
        final AtomicInteger firstReceived = new AtomicInteger();
        final AtomicInteger secondReceived = new AtomicInteger();
        Subscription first = proxy.getByteArrayAsStreamAsync().subscribe(new Action1<InputStream>() {
            @Override
            public void call(InputStream inputStream) {
                try {
                    while (inputStream.read() >= 0) {
                        firstReceived.incrementAndGet();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Subscription second = proxy.getByteArrayAsStreamAsync().subscribe(new Action1<InputStream>() {
            @Override
            public void call(InputStream inputStream) {
                try {
                    while (inputStream.read() >= 0) {
                        secondReceived.incrementAndGet();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread.sleep(5000);

        second.unsubscribe();
        first.unsubscribe();
        Assert.assertTrue(first.isUnsubscribed());
        Assert.assertTrue(second.isUnsubscribed());
        System.out.println("First received " + firstReceived.get());
        System.out.println("Second received " + secondReceived.get());
    }
}
