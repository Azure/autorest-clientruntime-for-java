package com.microsoft.rest.v3.http;

import com.microsoft.rest.v3.policy.HttpLogDetailLevel;
import com.microsoft.rest.v3.policy.HttpLoggingPolicy;
import com.microsoft.rest.v3.policy.PortPolicy;
import com.microsoft.rest.v3.policy.ProtocolPolicy;
import com.microsoft.rest.v3.policy.RetryPolicy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HttpPipelineBuilderTests {
    @Test
    public void constructorWithNoArguments() {
        final HttpPipelineBuilder builder = new HttpPipelineBuilder();
        assertEquals(0, builder.pipelinePolicies().size());
        assertNull(builder.options());
    }

    @Test
    public void withRequestPolicy() {
        final HttpPipelineBuilder builder = new HttpPipelineBuilder();

        builder.withPolicy(new PortPolicy(80));
        assertEquals(1, builder.pipelinePolicies().size());
        assertEquals(PortPolicy.class, builder.pipelinePolicies().get(0).getClass());

        builder.withPolicy(new ProtocolPolicy("ftp"));
        assertEquals(2, builder.pipelinePolicies().size());
        assertEquals(PortPolicy.class, builder.pipelinePolicies().get(0).getClass());
        assertEquals(ProtocolPolicy.class, builder.pipelinePolicies().get(1).getClass());

        builder.withPolicy(new RetryPolicy());
        assertEquals(3, builder.pipelinePolicies().size());
        assertEquals(PortPolicy.class, builder.pipelinePolicies().get(0).getClass());
        assertEquals(ProtocolPolicy.class, builder.pipelinePolicies().get(1).getClass());
        assertEquals(RetryPolicy.class, builder.pipelinePolicies().get(2).getClass());
    }

    @Test
    public void withRequestPolicyWithIndex() {
        final HttpPipelineBuilder builder = new HttpPipelineBuilder();

        builder.withPolicy(0, new PortPolicy(80));
        assertEquals(1, builder.pipelinePolicies().size());
        assertEquals(PortPolicy.class, builder.pipelinePolicies().get(0).getClass());

        builder.withPolicy(0, new ProtocolPolicy("ftp"));
        assertEquals(2, builder.pipelinePolicies().size());
        assertEquals(ProtocolPolicy.class, builder.pipelinePolicies().get(0).getClass());
        assertEquals(PortPolicy.class, builder.pipelinePolicies().get(1).getClass());

        builder.withPolicy(1, new RetryPolicy());
        assertEquals(3, builder.pipelinePolicies().size());
        assertEquals(ProtocolPolicy.class, builder.pipelinePolicies().get(0).getClass());
        assertEquals(RetryPolicy.class, builder.pipelinePolicies().get(1).getClass());
        assertEquals(PortPolicy.class, builder.pipelinePolicies().get(2).getClass());
    }

    @Test
    public void withRequestPolicyArray() {
        final HttpPipelineBuilder builder = new HttpPipelineBuilder();

        builder.withPolicies(
                new ProtocolPolicy("http"),
                new PortPolicy(80),
                new HttpLoggingPolicy(HttpLogDetailLevel.BODY));

        assertEquals(3, builder.pipelinePolicies().size());
        assertEquals(ProtocolPolicy.class, builder.pipelinePolicies().get(0).getClass());
        assertEquals(PortPolicy.class, builder.pipelinePolicies().get(1).getClass());
        assertEquals(HttpLoggingPolicy.class, builder.pipelinePolicies().get(2).getClass());
    }

    @Test
    public void appendingRequestPolicyArray() {
        final HttpPipelineBuilder builder = new HttpPipelineBuilder();

        builder.withPolicy(new RetryPolicy());
        builder.withPolicies(
                new ProtocolPolicy("http"),
                new PortPolicy(80),
                new HttpLoggingPolicy(HttpLogDetailLevel.BODY));

        assertEquals(4, builder.pipelinePolicies().size());
        assertEquals(RetryPolicy.class, builder.pipelinePolicies().get(0).getClass());
        assertEquals(ProtocolPolicy.class, builder.pipelinePolicies().get(1).getClass());
        assertEquals(PortPolicy.class, builder.pipelinePolicies().get(2).getClass());
        assertEquals(HttpLoggingPolicy.class, builder.pipelinePolicies().get(3).getClass());
    }
}
