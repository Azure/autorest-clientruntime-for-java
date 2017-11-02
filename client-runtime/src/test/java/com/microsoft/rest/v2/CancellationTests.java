/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2;


import com.microsoft.rest.v2.annotations.ExpectedResponses;
import com.microsoft.rest.v2.annotations.GET;
import com.microsoft.rest.v2.annotations.Host;
import com.microsoft.rest.v2.annotations.PathParam;
import com.microsoft.rest.v2.http.HttpClient;
import com.microsoft.rest.v2.http.HttpClient.Configuration;
import com.microsoft.rest.v2.http.NettyClient;
import com.microsoft.rest.v2.policy.RequestPolicy;
import com.microsoft.rest.v2.serializer.JacksonAdapter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;
import rx.Single;
import rx.Subscriber;
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
        httpClient = new NettyClient.Factory().create(new Configuration(new ArrayList<RequestPolicy.Factory>(), null));
        proxy = RestProxy.create(Service.class, null, httpClient, new JacksonAdapter());
    }

    @Host("http://httpbin.org")
    private interface Service {
        @GET("bytes/{length}")
        @ExpectedResponses({200})
        Single<byte[]> getByteArrayAsync(@PathParam("length") int length);

        @GET("bytes/{length}")
        @ExpectedResponses({200})
        Single<RestResponse<Void, Observable<byte[]>>> getByteArrayAsStreamAsync(@PathParam("length") int length);
    }

    @Test
    public void cancelOperationInProgress() throws Exception {
        final AtomicBoolean finished = new AtomicBoolean(false);
        Subscription subscription = proxy.getByteArrayAsync(102400)
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
        Subscription subscription = proxy.getByteArrayAsStreamAsync(102400).toObservable()
                .flatMap(new Func1<RestResponse<Void, Observable<byte[]>>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(RestResponse<Void, Observable<byte[]>> voidObservableRestResponse) {
                        return voidObservableRestResponse.body();
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
        Subscription first = proxy.getByteArrayAsStreamAsync(102400).toObservable()
                .flatMap(new Func1<RestResponse<Void, Observable<byte[]>>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(RestResponse<Void, Observable<byte[]>> voidObservableRestResponse) {
                        return voidObservableRestResponse.body();
                    }
                })
                .subscribe(new Action1<byte[]>() {
                    @Override
                    public void call(byte[] bytes) {
                        firstReceived.addAndGet(bytes.length);
                    }
                });


        Thread.sleep(500);

        Subscription second = proxy.getByteArrayAsStreamAsync(102400).toObservable()
                .flatMap(new Func1<RestResponse<Void, Observable<byte[]>>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(RestResponse<Void, Observable<byte[]>> voidObservableRestResponse) {
                        return voidObservableRestResponse.body();
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

    @Test
    public void cancelOperationsInParallel() throws Exception {
        final AtomicInteger received = new AtomicInteger();
        Single<RestResponse<Void, Observable<byte[]>>> first = proxy.getByteArrayAsStreamAsync(102400);
        Single<RestResponse<Void, Observable<byte[]>>> second = proxy.getByteArrayAsStreamAsync(65536);

        final CountDownLatch latch = new CountDownLatch(1);
        Subscription subscription = first.mergeWith(second)
                .flatMap(new Func1<RestResponse<Void, Observable<byte[]>>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(RestResponse<Void, Observable<byte[]>> voidObservableRestResponse) {
                        return voidObservableRestResponse.body();
                    }
                })
                .subscribe(new Action1<byte[]>() {
                    @Override
                    public void call(byte[] bytes) {
                        received.addAndGet(bytes.length);
                        if (received.get() >= 102400) {
                            latch.countDown();
                        }
                    }
                });

        latch.await();

//        subscription.unsubscribe();
//        Assert.assertTrue(subscription.isUnsubscribed());
//        int partial = received.get();
//
//        Thread.sleep(5000);
//        Assert.assertEquals(partial, received.get());
    }
}
