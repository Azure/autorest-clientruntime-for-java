/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2;

import com.microsoft.rest.RestClient;
import com.microsoft.rest.v2.annotations.GET;
import com.microsoft.rest.v2.annotations.Host;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RestProxy implements InvocationHandler {
    private String host;
    private RestClient restClient;

    private RestProxy(String host, RestClient restClient) {
        this.host = host;
        this.restClient = restClient;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isAnnotationPresent(GET.class)) {
            String relativePath = method.getAnnotation(GET.class).value();
            return (host + "/" + relativePath).getBytes();
        }
        throw new NotImplementedException();
    }

    @SuppressWarnings("unchecked")
    public static <A> A create(Class<A> actionable, RestClient restClient) {
        String host = restClient.retrofit().baseUrl().host();
        if (actionable.isAnnotationPresent(Host.class)) {
            host = actionable.getAnnotation(Host.class).value();
        }
        return (A) Proxy.newProxyInstance(actionable.getClassLoader(), new Class[] { actionable }, new RestProxy(host, restClient));
    }
}