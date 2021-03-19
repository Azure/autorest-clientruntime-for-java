/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.serializer.AzureJacksonAdapter;
import com.microsoft.rest.protocol.SerializerAdapter;
import org.junit.Assert;
import org.junit.Test;

public class CloudErrorDeserializerTests {
    @Test
    public void cloudErrorDeserialization() throws Exception {
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
            "        \"additionalInfo\": [" +
            "            {" +
            "                \"type\": \"SomeErrorType\"," +
            "                \"info\": {" +
            "                    \"someProperty\": \"SomeValue\"" +
            "                }" +
            "            }" +
            "        ]" +
            "    }" +
            "}";
        
        CloudError cloudError = serializerAdapter.deserialize(bodyString, CloudError.class);

        Assert.assertEquals("BadArgument", cloudError.code());
        Assert.assertEquals("The provided database ‘foo’ has an invalid username.", cloudError.message());
        Assert.assertEquals("query", cloudError.target());
        Assert.assertEquals(1, cloudError.details().size());
        Assert.assertEquals("301", cloudError.details().get(0).code());
        Assert.assertEquals(1, cloudError.additionalInfo().size());
        Assert.assertEquals("SomeErrorType", cloudError.additionalInfo().get(0).type());
        Assert.assertEquals("SomeValue", cloudError.additionalInfo().get(0).info().get("someProperty").asText());
    }
    
    @Test
    public void cloudErrorWithPolicyViolationDeserialization() throws Exception {
        SerializerAdapter<ObjectMapper> serializerAdapter = new AzureJacksonAdapter();
        String bodyString =
            "{" +
            "    \"error\": {" +
            "        \"code\": \"BadArgument\"," +
            "        \"message\": \"The provided database ‘foo’ has an invalid username.\"," +
            "        \"target\": \"query\"," +
            "        \"details\": [" +
            "        {" +
            "            \"code\": \"301\"," +
            "            \"target\": \"$search\"," +
            "            \"message\": \"$search query option not supported\"," +
            "            \"additionalInfo\": [" +
            "            {" +
            "                \"type\": \"PolicyViolation\"," +
            "                \"info\": {" +
            "                    \"policyDefinitionDisplayName\": \"Allowed locations\"," +
            "                    \"policyDefinitionId\": \"testDefinitionId\"," +
            "                    \"policyDefinitionName\": \"testDefinitionName\"," +
            "                    \"policyDefinitionEffect\": \"deny\"," +
            "                    \"policyAssignmentId\": \"testAssignmentId\"," +
            "                    \"policyAssignmentName\": \"testAssignmentName\"," +
            "                    \"policyAssignmentDisplayName\": \"test assignment\"," +
            "                    \"policyAssignmentScope\": \"/subscriptions/testSubId/resourceGroups/jilimpolicytest2\"," +
            "                    \"policyAssignmentParameters\": {" +
            "                        \"listOfAllowedLocations\": {" +
            "                            \"value\": [" +
            "                                \"westus\"" +
            "                	         ]" +
            "                    	 }" +
            "                    }" +            
            "                }" +
            "            }" +
            "            ]" +
            "        }" +
            "        ]," +
            "        \"additionalInfo\": [" +
            "            {" +
            "                \"type\": \"SomeErrorType\"," +
            "                \"info\": {" +
            "                    \"someProperty\": \"SomeValue\"" +
            "                }" +
            "            }" +
            "        ]" +
            "    }" +
            "}";
        
        CloudError cloudError = serializerAdapter.deserialize(bodyString, CloudError.class);

        Assert.assertEquals("BadArgument", cloudError.code());
        Assert.assertEquals("The provided database ‘foo’ has an invalid username.", cloudError.message());
        Assert.assertEquals("query", cloudError.target());
        Assert.assertEquals(1, cloudError.details().size());
        Assert.assertEquals("301", cloudError.details().get(0).code());
        Assert.assertEquals(1, cloudError.additionalInfo().size());
        Assert.assertEquals("SomeErrorType", cloudError.additionalInfo().get(0).type());
        Assert.assertEquals("SomeValue", cloudError.additionalInfo().get(0).info().get("someProperty").asText());
        Assert.assertEquals(1, cloudError.details().get(0).additionalInfo().size());
        Assert.assertTrue(cloudError.details().get(0).additionalInfo().get(0) instanceof PolicyViolation);
        
        PolicyViolation policyViolation = (PolicyViolation)cloudError.details().get(0).additionalInfo().get(0);
        
        Assert.assertEquals("PolicyViolation", policyViolation.type());
        Assert.assertEquals("Allowed locations", policyViolation.policyErrorInfo().getPolicyDefinitionDisplayName());
        Assert.assertEquals("westus", policyViolation.policyErrorInfo().getPolicyAssignmentParameters().get("listOfAllowedLocations").getValue().elements().next().asText());
    }
    
    @Test
    public void cloudErrorWitDifferentCasing() throws Exception {
        SerializerAdapter<ObjectMapper> serializerAdapter = new AzureJacksonAdapter();
        String bodyString =
            "{" +
            "    \"error\": {" +
            "        \"Code\": \"BadArgument\"," +
            "        \"Message\": \"The provided database ‘foo’ has an invalid username.\"," +
            "        \"Target\": \"query\"," +
            "        \"Details\": [" +
            "        {" +
            "            \"Code\": \"301\"," +
            "            \"Target\": \"$search\"," +
            "            \"Message\": \"$search query option not supported\"," +
            "            \"AdditionalInfo\": [" +
            "            {" +
            "                \"Type\": \"PolicyViolation\"," +
            "                \"Info\": {" +
            "                    \"PolicyDefinitionDisplayName\": \"Allowed locations\"," +
            "                    \"PolicyAssignmentParameters\": {" +
            "                        \"listOfAllowedLocations\": {" +
            "                            \"Value\": [" +
            "                                \"westus\"" +
            "                	         ]" +
            "                    	 }" +
            "                    }" +            
            "                }" +
            "            }" +
            "            ]" +
            "        }" +
            "        ]" +
            "    }" +
            "}";
        
        CloudError cloudError = serializerAdapter.deserialize(bodyString, CloudError.class);

        Assert.assertEquals("BadArgument", cloudError.code());
        Assert.assertEquals("The provided database ‘foo’ has an invalid username.", cloudError.message());
        Assert.assertEquals("query", cloudError.target());
        Assert.assertEquals(1, cloudError.details().size());
        Assert.assertEquals("301", cloudError.details().get(0).code());
        Assert.assertEquals(1, cloudError.details().get(0).additionalInfo().size());
        Assert.assertTrue(cloudError.details().get(0).additionalInfo().get(0) instanceof PolicyViolation);
        
        PolicyViolation policyViolation = (PolicyViolation)cloudError.details().get(0).additionalInfo().get(0);
        
        Assert.assertEquals("PolicyViolation", policyViolation.type());
        Assert.assertEquals("Allowed locations", policyViolation.policyErrorInfo().getPolicyDefinitionDisplayName());
        Assert.assertEquals("westus", policyViolation.policyErrorInfo().getPolicyAssignmentParameters().get("listOfAllowedLocations").getValue().elements().next().asText());
    }

    @Test
    public void testEvaluationDetails() throws IOException {
        SerializerAdapter<ObjectMapper> serializerAdapter = new AzureJacksonAdapter();
        String bodyString = "{\n"
                + "  \"error\": {\n"
                + "    \"code\": \"RequestDisallowedByPolicy\",\n"
                + "    \"target\": \"apim-NorthWindShoes346878645\",\n"
                + "    \"message\": \"Resource 'apim-NorthWindShoes346878645' was disallowed by policy. (Code: RequestDisallowedByPolicy)\",\n"
                + "    \"additionalInfo\": [\n"
                + "      {\n"
                + "        \"type\": \"PolicyViolation\",\n"
                + "        \"info\": {\n"
                + "          \"policyDefinitionDisplayName\": \"Allowed resource types\",\n"
                + "          \"policySetDefinitionDisplayName\": \"webapp\",\n"
                + "          \"evaluationDetails\": {\n"
                + "            \"evaluatedExpressions\": [\n"
                + "              {\n"
                + "                \"result\": \"False\",\n"
                + "                \"expression\": \"type\",\n"
                + "                \"path\": \"type\",\n"
                + "                \"expressionValue\": \"Microsoft.ApiManagement/service\",\n"
                + "                \"targetValue\": [\n"
                + "                  \"Microsoft.Resources/resourceGroups\",\n"
                + "                  \"Microsoft.Storage/storageAccounts\",\n"
                + "                  \"Microsoft.Web/sites\",\n"
                + "                  \"Microsoft.Web/serverFarms\",\n"
                + "                  \"Microsoft.Web/functions\",\n"
                + "                  \"Microsoft.DocumentDB/databaseAccounts\",\n"
                + "                  \"microsoft.insights/components\",\n"
                + "                  \"Microsoft.KeyVault/vaults\",\n"
                + "                  \"Microsoft.Cache/Redis\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/authorizationrules\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/queues\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/queues/authorizationrules\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/topics\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/topics/authorizationrules\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/topics/subscriptions\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/topics/subscriptions/rules\",\n"
                + "                  \"Microsoft.CognitiveServices/accounts\",\n"
                + "                  \"Microsoft.Web/sites/slots\",\n"
                + "                  \"Microsoft.Web/sites/slots/instances\",\n"
                + "                  \"Microsoft.Web/sites/slots/metrics\",\n"
                + "                  \"Microsoft.Web/sites/metrics\",\n"
                + "                  \"Microsoft.Web/sites/instances\",\n"
                + "                  \"Microsoft.Web/certificates\",\n"
                + "                  \"Microsoft.Portal/dashboards\",\n"
                + "                  \"Microsoft.ContainerRegistry/registries\",\n"
                + "                  \"Microsoft.ContainerRegistry/registries/webhooks\",\n"
                + "                  \"Microsoft.Web/connections\",\n"
                + "                  \"Microsoft.Logic/workflows\",\n"
                + "                  \"Microsoft.Web/customApis\",\n"
                + "                  \"Microsoft.Search/searchServices\",\n"
                + "                  \"Microsoft.Network/trafficmanagerprofiles\",\n"
                + "                  \"Microsoft.Sql/servers\",\n"
                + "                  \"Microsoft.Sql/servers/databases\",\n"
                + "                  \"Microsoft.SignalRService/SignalR\"\n"
                + "                ],\n"
                + "                \"operator\": \"In\"\n"
                + "              }\n"
                + "            ]\n"
                + "          },\n"
                + "          \"policyDefinitionId\": \"/providers/Microsoft.Authorization/policyDefinitions/a08ec900-254a-4555-9bf5-e42af04b5c5c\",\n"
                + "          \"policySetDefinitionId\": \"/providers/Microsoft.Management/managementGroups/triplecrown2/providers/Microsoft.Authorization/policySetDefinitions/f06460e7-86f9-42ad-8955-fb55bad92028\",\n"
                + "          \"policyDefinitionReferenceId\": \"8272972042953317437\",\n"
                + "          \"policySetDefinitionName\": \"f06460e7-86f9-42ad-8955-fb55bad92028\",\n"
                + "          \"policyDefinitionName\": \"a08ec900-254a-4555-9bf5-e42af04b5c5c\",\n"
                + "          \"policyDefinitionEffect\": \"deny\",\n"
                + "          \"policyAssignmentId\": \"/subscriptions/<subscription-id>/resourceGroups/<resouce-group>/providers/Microsoft.Authorization/policyAssignments/TripleCrownPolicy\",\n"
                + "          \"policyAssignmentName\": \"TripleCrownPolicy\",\n"
                + "          \"policyAssignmentDisplayName\": \"Sandbox Policy\",\n"
                + "          \"policyAssignmentScope\": \"/subscriptions/<subscription-id>/resourceGroups/<resouce-group>\"\n"
                + "        }\n"
                + "      },\n"
                + "      {\n"
                + "        \"type\": \"PolicyViolation\",\n"
                + "        \"info\": {\n"
                + "          \"policyDefinitionDisplayName\": \"Allowed resource types\",\n"
                + "          \"policySetDefinitionDisplayName\": \"webapp\",\n"
                + "          \"evaluationDetails\": {\n"
                + "            \"evaluatedExpressions\": [\n"
                + "              {\n"
                + "                \"result\": \"False\",\n"
                + "                \"expression\": \"type\",\n"
                + "                \"path\": \"type\",\n"
                + "                \"expressionValue\": \"Microsoft.ApiManagement/service\",\n"
                + "                \"targetValue\": [\n"
                + "                  \"Microsoft.Resources/resourceGroups\",\n"
                + "                  \"Microsoft.Storage/storageAccounts\",\n"
                + "                  \"Microsoft.Web/sites\",\n"
                + "                  \"Microsoft.Web/serverFarms\",\n"
                + "                  \"Microsoft.Web/functions\",\n"
                + "                  \"Microsoft.DocumentDB/databaseAccounts\",\n"
                + "                  \"microsoft.insights/components\",\n"
                + "                  \"Microsoft.KeyVault/vaults\",\n"
                + "                  \"Microsoft.Cache/Redis\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/authorizationrules\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/queues\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/queues/authorizationrules\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/topics\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/topics/authorizationrules\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/topics/subscriptions\",\n"
                + "                  \"Microsoft.ServiceBus/namespaces/topics/subscriptions/rules\",\n"
                + "                  \"Microsoft.CognitiveServices/accounts\",\n"
                + "                  \"Microsoft.Web/sites/slots\",\n"
                + "                  \"Microsoft.Web/sites/slots/instances\",\n"
                + "                  \"Microsoft.Web/sites/slots/metrics\",\n"
                + "                  \"Microsoft.Web/sites/metrics\",\n"
                + "                  \"Microsoft.Web/sites/instances\",\n"
                + "                  \"Microsoft.Web/certificates\",\n"
                + "                  \"Microsoft.Portal/dashboards\",\n"
                + "                  \"Microsoft.ContainerRegistry/registries\",\n"
                + "                  \"Microsoft.ContainerRegistry/registries/webhooks\",\n"
                + "                  \"Microsoft.Web/connections\",\n"
                + "                  \"Microsoft.Logic/workflows\",\n"
                + "                  \"Microsoft.Web/customApis\",\n"
                + "                  \"Microsoft.Search/searchServices\",\n"
                + "                  \"Microsoft.Network/trafficmanagerprofiles\",\n"
                + "                  \"Microsoft.Sql/servers\",\n"
                + "                  \"Microsoft.Sql/servers/databases\",\n"
                + "                  \"Microsoft.SignalRService/SignalR\"\n"
                + "                ],\n"
                + "                \"operator\": \"In\"\n"
                + "              }\n"
                + "            ]\n"
                + "          },\n"
                + "          \"policyDefinitionId\": \"/providers/Microsoft.Authorization/policyDefinitions/a08ec900-254a-4555-9bf5-e42af04b5c5c\",\n"
                + "          \"policySetDefinitionId\": \"/providers/Microsoft.Management/managementGroups/triplecrown2/providers/Microsoft.Authorization/policySetDefinitions/f06460e7-86f9-42ad-8955-fb55bad92028\",\n"
                + "          \"policyDefinitionReferenceId\": \"8272972042953317437\",\n"
                + "          \"policySetDefinitionName\": \"f06460e7-86f9-42ad-8955-fb55bad92028\",\n"
                + "          \"policyDefinitionName\": \"a08ec900-254a-4555-9bf5-e42af04b5c5c\",\n"
                + "          \"policyDefinitionEffect\": \"deny\",\n"
                + "          \"policyAssignmentId\": \"/providers/Microsoft.Management/managementGroups/triplecrown2/providers/Microsoft.Authorization/policyAssignments/4a0a4629d22043e6a70a69f9\",\n"
                + "          \"policyAssignmentName\": \"4a0a4629d22043e6a70a69f9\",\n"
                + "          \"policyAssignmentDisplayName\": \"webapp\",\n"
                + "          \"policyAssignmentScope\": \"/providers/Microsoft.Management/managementGroups/triplecrown2\",\n"
                + "          \"policyAssignmentParameters\": {}\n"
                + "        }\n"
                + "      }\n"
                + "    ],\n"
                + "    \"policyDetails\": [\n"
                + "      {\n"
                + "        \"isInitiative\": true,\n"
                + "        \"assignmentId\": \"/subscriptions/<subscription-id>/resourceGroups/<resouce-group>/providers/Microsoft.Authorization/policyAssignments/TripleCrownPolicy\",\n"
                + "        \"assignmentName\": \"Sandbox Policy\",\n"
                + "        \"auxDefinitionNames\": [\n"
                + "          \"Allowed resource types\"\n"
                + "        ],\n"
                + "        \"viewDetailsUri\": \"https://portal.azure.com#blade/Microsoft_Azure_Policy/EditAssignmentBlade/id/%2Fsubscriptions%2F<subscription-id>%2FresourceGroups%2F<resouce-group>%2Fproviders%2FMicrosoft.Authorization%2FpolicyAssignments%2FTripleCrownPolicy\"\n"
                + "      },\n"
                + "      {\n"
                + "        \"isInitiative\": true,\n"
                + "        \"assignmentId\": \"/providers/Microsoft.Management/managementGroups/triplecrown2/providers/Microsoft.Authorization/policyAssignments/4a0a4629d22043e6a70a69f9\",\n"
                + "        \"assignmentName\": \"webapp\",\n"
                + "        \"auxDefinitionNames\": [\n"
                + "          \"Allowed resource types\"\n"
                + "        ],\n"
                + "        \"viewDetailsUri\": \"https://portal.azure.com#blade/Microsoft_Azure_Policy/EditAssignmentBlade/id/%2Fproviders%2FMicrosoft.Management%2FmanagementGroups%2Ftriplecrown2%2Fproviders%2FMicrosoft.Authorization%2FpolicyAssignments%2F4a0a4629d22043e6a70a69f9\"\n"
                + "      }\n"
                + "    ]\n"
                + "  }\n"
                + "}";

        CloudError cloudError = serializerAdapter.deserialize(bodyString, CloudError.class);

        Assert.assertEquals("RequestDisallowedByPolicy", cloudError.code());
        Assert.assertEquals("Resource 'apim-NorthWindShoes346878645' was disallowed by policy. (Code: RequestDisallowedByPolicy)", cloudError.message());
        Assert.assertEquals("apim-NorthWindShoes346878645", cloudError.target());
        Assert.assertEquals(0, cloudError.details().size());
        Assert.assertEquals(2, cloudError.additionalInfo().size());
        Assert.assertTrue(cloudError.additionalInfo().get(0) instanceof PolicyViolation);

        PolicyViolation policyViolation = (PolicyViolation)cloudError.additionalInfo().get(0);
        PolicyViolationErrorInfo policyViolationErrorInfo = policyViolation.policyErrorInfo();

        Assert.assertEquals("Allowed resource types", policyViolationErrorInfo.getPolicyDefinitionDisplayName());
        Assert.assertEquals("webapp", policyViolationErrorInfo.getPolicySetDefinitionDisplayName());
        Assert.assertEquals("/providers/Microsoft.Authorization/policyDefinitions/a08ec900-254a-4555-9bf5-e42af04b5c5c",
                policyViolationErrorInfo.getPolicyDefinitionId());
        Assert.assertEquals("/providers/Microsoft.Management/managementGroups/triplecrown2/providers/Microsoft.Authorization/policySetDefinitions/"
                        + "f06460e7-86f9-42ad-8955-fb55bad92028", policyViolationErrorInfo.getPolicySetDefinitionId());
        Assert.assertEquals("8272972042953317437", policyViolationErrorInfo.getPolicyDefinitionReferenceId());
        Assert.assertEquals("f06460e7-86f9-42ad-8955-fb55bad92028", policyViolationErrorInfo.getPolicySetDefinitionName());
        Assert.assertEquals("a08ec900-254a-4555-9bf5-e42af04b5c5c", policyViolationErrorInfo.getPolicyDefinitionName());
        Assert.assertEquals("deny", policyViolationErrorInfo.getPolicyDefinitionEffect());
        Assert.assertEquals("/subscriptions/<subscription-id>/resourceGroups/<resouce-group>/providers/Microsoft.Authorization/policyAssignments/"
                + "TripleCrownPolicy", policyViolationErrorInfo.getPolicyAssignmentId());
        Assert.assertEquals("TripleCrownPolicy", policyViolationErrorInfo.getPolicyAssignmentName());
        Assert.assertEquals("Sandbox Policy", policyViolationErrorInfo.getPolicyAssignmentDisplayName());
        Assert.assertEquals("/subscriptions/<subscription-id>/resourceGroups/<resouce-group>", policyViolationErrorInfo.getPolicyAssignmentScope());

        List<ExpressionEvaluationDetails> evaluatedExpressions = policyViolationErrorInfo.getEvaluationDetails().getEvaluatedExpressions();

        Assert.assertEquals(1, evaluatedExpressions.size());

        ExpressionEvaluationDetails expressionEvaluationDetails = evaluatedExpressions.get(0);

        Assert.assertEquals("False", expressionEvaluationDetails.getResult());
        Assert.assertEquals("type", expressionEvaluationDetails.getExpression());
        Assert.assertEquals("type", expressionEvaluationDetails.getPath());
        Assert.assertEquals("Microsoft.ApiManagement/service", expressionEvaluationDetails.getExpressionValue());
        Assert.assertEquals("In", expressionEvaluationDetails.getOperator());
        Assert.assertEquals(35, expressionEvaluationDetails.getTargetValue().size());
        Assert.assertTrue(expressionEvaluationDetails.getTargetValue().contains("Microsoft.Web/certificates"));
    }
}