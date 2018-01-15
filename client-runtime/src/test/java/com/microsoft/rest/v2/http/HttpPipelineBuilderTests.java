package com.microsoft.rest.v2.http;

import com.microsoft.rest.v2.policy.LoggingPolicy;
import com.microsoft.rest.v2.policy.PortPolicy;
import com.microsoft.rest.v2.policy.ProtocolPolicyFactory;
import com.microsoft.rest.v2.policy.RetryPolicyFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HttpPipelineBuilderTests {
    @Test
    public void constructorWithNoArguments() {
        final HttpPipelineBuilder builder = new HttpPipelineBuilder();
        assertEquals(0, builder.requestPolicyFactories().size());
        assertNull(builder.options());
    }

    @Test
    public void withRequestPolicy() {
        final HttpPipelineBuilder builder = new HttpPipelineBuilder();

        builder.withRequestPolicy(new PortPolicy.Factory(80));
        assertEquals(1, builder.requestPolicyFactories().size());
        assertEquals(PortPolicy.Factory.class, builder.requestPolicyFactories().get(0).getClass());

        builder.withRequestPolicy(new ProtocolPolicyFactory("ftp"));
        assertEquals(2, builder.requestPolicyFactories().size());
        assertEquals(ProtocolPolicyFactory.class, builder.requestPolicyFactories().get(0).getClass());
        assertEquals(PortPolicy.Factory.class, builder.requestPolicyFactories().get(1).getClass());

        builder.withRequestPolicy(new RetryPolicyFactory());
        assertEquals(3, builder.requestPolicyFactories().size());
        assertEquals(RetryPolicyFactory.class, builder.requestPolicyFactories().get(0).getClass());
        assertEquals(ProtocolPolicyFactory.class, builder.requestPolicyFactories().get(1).getClass());
        assertEquals(PortPolicy.Factory.class, builder.requestPolicyFactories().get(2).getClass());
    }

    @Test
    public void withRequestPolicyWithIndex() {
        final HttpPipelineBuilder builder = new HttpPipelineBuilder();

        builder.withRequestPolicy(0, new PortPolicy.Factory(80));
        assertEquals(1, builder.requestPolicyFactories().size());
        assertEquals(PortPolicy.Factory.class, builder.requestPolicyFactories().get(0).getClass());

        builder.withRequestPolicy(0, new ProtocolPolicyFactory("ftp"));
        assertEquals(2, builder.requestPolicyFactories().size());
        assertEquals(PortPolicy.Factory.class, builder.requestPolicyFactories().get(0).getClass());
        assertEquals(ProtocolPolicyFactory.class, builder.requestPolicyFactories().get(1).getClass());

        builder.withRequestPolicy(1, new RetryPolicyFactory());
        assertEquals(3, builder.requestPolicyFactories().size());
        assertEquals(PortPolicy.Factory.class, builder.requestPolicyFactories().get(0).getClass());
        assertEquals(RetryPolicyFactory.class, builder.requestPolicyFactories().get(1).getClass());
        assertEquals(ProtocolPolicyFactory.class, builder.requestPolicyFactories().get(2).getClass());
    }

    @Test
    public void withRequestPolicyArray() {
        final HttpPipelineBuilder builder = new HttpPipelineBuilder();

        builder.withRequestPolicies(
                new ProtocolPolicyFactory("http"),
                new PortPolicy.Factory(80),
                new LoggingPolicy.Factory(LoggingPolicy.LogLevel.BODY));

        assertEquals(3, builder.requestPolicyFactories().size());
        assertEquals(LoggingPolicy.Factory.class, builder.requestPolicyFactories().get(0).getClass());
        assertEquals(PortPolicy.Factory.class, builder.requestPolicyFactories().get(1).getClass());
        assertEquals(ProtocolPolicyFactory.class, builder.requestPolicyFactories().get(2).getClass());
    }

    @Test
    public void appendingRequestPolicyArray() {
        final HttpPipelineBuilder builder = new HttpPipelineBuilder();

        builder.withRequestPolicy(new RetryPolicyFactory());
        builder.withRequestPolicies(
                new ProtocolPolicyFactory("http"),
                new PortPolicy.Factory(80),
                new LoggingPolicy.Factory(LoggingPolicy.LogLevel.BODY));

        assertEquals(4, builder.requestPolicyFactories().size());
        assertEquals(LoggingPolicy.Factory.class, builder.requestPolicyFactories().get(0).getClass());
        assertEquals(PortPolicy.Factory.class, builder.requestPolicyFactories().get(1).getClass());
        assertEquals(ProtocolPolicyFactory.class, builder.requestPolicyFactories().get(2).getClass());
        assertEquals(RetryPolicyFactory.class, builder.requestPolicyFactories().get(3).getClass());

    }
}
