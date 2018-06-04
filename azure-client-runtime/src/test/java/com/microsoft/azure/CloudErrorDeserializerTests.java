/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.serializer.AzureJacksonAdapter;
import com.microsoft.rest.protocol.SerializerAdapter;
import org.junit.Assert;
import org.junit.Test;

public class CloudErrorDeserializerTests {
    @Test
    public void errorDeserializedFully() throws Exception {
        SerializerAdapter<ObjectMapper> serializerAdapter = new AzureJacksonAdapter();
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
        CloudError cloudError = serializerAdapter.deserialize(bodyString, CloudError.class);

        Assert.assertEquals("BadArgument", cloudError.code());
        Assert.assertEquals("The provided database ‘foo’ has an invalid username.", cloudError.message());
        Assert.assertEquals("query", cloudError.target());
        Assert.assertEquals("301", cloudError.details().get(0).code());
        Assert.assertEquals("customValue", cloudError.innerError().get("customKey").asText());
    }
}
