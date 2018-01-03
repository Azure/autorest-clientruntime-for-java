/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure;

import com.microsoft.azure.serializer.AzureJacksonAdapter;
import com.microsoft.rest.interceptors.RequestIdHeaderInterceptor;
import com.microsoft.rest.RestClient;
import org.junit.Assert;
import org.junit.Test;

public class CloudErrorDeserializerTests {
    @Test
    public void errorDeserializedFully() throws Exception {
        RestClient restClient = new RestClient.Builder()
                .withBaseUrl("http://localhost")
                .withSerializerAdapter(new AzureJacksonAdapter())
                .withResponseBuilderFactory(new AzureResponseBuilder.Factory())
                .withInterceptor(new RequestIdHeaderInterceptor())
                .build();
        String bodyString =
            "{" +
            "    \"error\": {" +
            "        \"code\": \"BadArgument\"," +
            "        \"message\": \"The provided database ‘foo’ has an invalid username.\"," +
            "        \"target\": \"query\"," +
            "        \"details\": [" +
            "            {" +
            "                \"code\": \"301\"," +
            "                \"target\": \"$search\"," +
            "                \"message\": \"$search query option not supported\"" +
            "            }" +
            "        ]," +
            "        \"innererror\": {" +
            "            \"customKey\": \"customValue\"" +
            "        }" +
            "    }" +
            "}";
        CloudError cloudError = restClient.serializerAdapter().deserialize(bodyString, CloudError.class);

        Assert.assertEquals("BadArgument", cloudError.code());
        Assert.assertEquals("The provided database ‘foo’ has an invalid username.", cloudError.message());
        Assert.assertEquals("query", cloudError.target());
        Assert.assertEquals("301", cloudError.details().get(0).code());
        Assert.assertEquals("customValue", cloudError.innererror().get("customKey").asText());
    }
}
