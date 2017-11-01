/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest;


import com.microsoft.rest.annotations.ExpectedResponses;
import com.microsoft.rest.annotations.GET;
import com.microsoft.rest.annotations.Host;
import com.microsoft.rest.http.HttpClient;
import com.microsoft.rest.http.HttpClient.Configuration;
import com.microsoft.rest.http.NettyClient;
import com.microsoft.rest.policy.RequestPolicy.Factory;
import com.microsoft.rest.serializer.JacksonAdapter;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.Single;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CancellationTests {

    private HttpClient httpClient;
    private Service proxy;

    public CancellationTests() {
        httpClient = new NettyClient.Factory(1, 1).create(new Configuration(new ArrayList<Factory>(), null));
        proxy = RestProxy.create(Service.class, null, httpClient, new JacksonAdapter());
    }

    @Host("http://httpbin.org")
    private interface Service {
        @GET("bytes/102400")
        @ExpectedResponses({200})
        Single<byte[]> getByteArrayAsync();

        @GET("bytes/102400")
        @ExpectedResponses({200})
        Single<Observable<byte[]>> getByteArrayAsStreamAsync();
    }

    @Test
    public void cancelOperationInProgress() throws Exception {
        final AtomicBoolean finished = new AtomicBoolean(false);
        Subscription subscription = proxy.getByteArrayAsync()
                .subscribe(new Action1<byte[]>() {
                    @Override
                    public void call(byte[] voidRestResponse) {
                        finished.set(true);
                    }
                });

        Thread.sleep(100);

        subscription.unsubscribe();
        Assert.assertTrue(subscription.isUnsubscribed());

        Thread.sleep(1000);
        Assert.assertFalse(finished.get());
    }

    @Test
    public void cancelStreamingInProgress() throws Exception {
        final AtomicInteger received = new AtomicInteger();

        final CountDownLatch latch = new CountDownLatch(1);
        Subscription subscription = proxy.getByteArrayAsStreamAsync().toObservable()
                .flatMap(new Func1<Observable<byte[]>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(Observable<byte[]> observable) {
                        return observable;
                    }
                })
                .subscribe(new Action1<byte[]>() {
                    @Override
                    public void call(byte[] bytes) {
                        received.addAndGet(bytes.length);
                        if (received.get() >= 30000) {
                            latch.countDown();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });

        latch.await();

        subscription.unsubscribe();
        Assert.assertTrue(subscription.isUnsubscribed());
        int partial = received.get();

        Thread.sleep(1000);
        Assert.assertEquals(partial, received.get());
    }

    @Test
    public void cancelOperationBeforeSent() throws Exception {
        final AtomicInteger firstReceived = new AtomicInteger();
        final AtomicInteger secondReceived = new AtomicInteger();
        Subscription first = proxy.getByteArrayAsStreamAsync().toObservable()
                .flatMap(new Func1<Observable<byte[]>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(Observable<byte[]> observable) {
                        return observable;
                    }
                })
                .subscribe(new Action1<byte[]>() {
                    @Override
                    public void call(byte[] bytes) {
                        firstReceived.addAndGet(bytes.length);
                    }
                });


        Thread.sleep(500);

        Subscription second = proxy.getByteArrayAsStreamAsync().toObservable()
                .flatMap(new Func1<Observable<byte[]>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(Observable<byte[]> observable) {
                        return observable;
                    }
                })
                .subscribe(new Action1<byte[]>() {
                    @Override
                    public void call(byte[] bytes) {
                        secondReceived.addAndGet(bytes.length);
                    }
                });

        second.unsubscribe();
        first.unsubscribe();
        Assert.assertTrue(first.isUnsubscribed());
        Assert.assertTrue(second.isUnsubscribed());
        Assert.assertEquals(0, secondReceived.get());
    }
}
