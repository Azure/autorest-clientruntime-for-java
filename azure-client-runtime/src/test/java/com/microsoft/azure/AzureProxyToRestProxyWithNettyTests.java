package com.microsoft.azure;

import com.microsoft.rest.http.ChannelHandlerConfig;
import com.microsoft.rest.http.HttpClient;
import com.microsoft.rest.http.NettyAdapter;
import com.microsoft.rest.policy.RequestPolicy;

import java.util.Collections;

public class AzureProxyToRestProxyWithNettyTests extends AzureProxyToRestProxyTests {
    @Override
    protected HttpClient createHttpClient() {
        return new NettyAdapter(Collections.<RequestPolicy.Factory>emptyList(), Collections.<ChannelHandlerConfig>emptyList());
    }
}
