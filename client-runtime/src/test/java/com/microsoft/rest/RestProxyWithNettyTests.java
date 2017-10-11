package com.microsoft.rest;

import com.microsoft.rest.http.ChannelHandlerConfig;
import com.microsoft.rest.http.HttpClient;
import com.microsoft.rest.http.NettyAdapter;
import com.microsoft.rest.policy.LoggingPolicy;
import com.microsoft.rest.policy.RequestPolicy.Factory;

import java.util.ArrayList;
import java.util.Collections;

public class RestProxyWithNettyTests extends RestProxyTests {
    private static NettyAdapter adapter = new NettyAdapter(Collections.<Factory>emptyList(), Collections.<ChannelHandlerConfig>emptyList());

    @Override
    protected HttpClient createHttpClient() {
        return adapter;
    }
}
