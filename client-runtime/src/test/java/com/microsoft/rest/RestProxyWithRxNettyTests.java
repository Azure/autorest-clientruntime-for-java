package com.microsoft.rest;

import com.microsoft.rest.http.ChannelHandlerConfig;
import com.microsoft.rest.http.HttpClient;
import com.microsoft.rest.http.RxNettyAdapter;
import com.microsoft.rest.policy.LoggingPolicy;
import com.microsoft.rest.policy.RequestPolicy.Factory;

import java.util.ArrayList;
import java.util.Collections;

public class RestProxyWithRxNettyTests extends RestProxyTests {
    @Override
    protected HttpClient createHttpClient() {
        return new RxNettyAdapter(new ArrayList<Factory>() {{
            add(new LoggingPolicy.Factory(LogLevel.BODY_AND_HEADERS));
        }}, Collections.<ChannelHandlerConfig>emptyList());
    }
}
