/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.credentials;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.management.apigeneration.Beta;
import com.microsoft.azure.serializer.AzureJacksonAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Managed Service Identity token based credentials for use with a REST Service Client.
 */
@Beta
public class MSICredentials extends AzureTokenCredentials {
    private final String resource;
    private final int msiPort;
    private final AzureJacksonAdapter adapter;
    private String objectId;
    private String clientId;
    private String identityId;


    /**
     * Initializes a new instance of the MSICredentials.
     */
    public MSICredentials() {
        this(AzureEnvironment.AZURE);
    }

    /**
     * Initializes a new instance of the MSICredentials.
     *
     * @param environment the Azure environment to use
     */

    public MSICredentials(AzureEnvironment environment) {
        this(environment, 50342);
    }

    /**
     * Initializes a new instance of the MSICredentials.
     *
     * @param environment the Azure environment to use
     * @param msiPort the local port to retrieve token from
     */
    public MSICredentials(AzureEnvironment environment, int msiPort) {
        super(environment, null /** retrieving MSI token does not require tenant **/);
        this.resource = environment.resourceManagerEndpoint();
        this.msiPort = msiPort;
        this.adapter = new AzureJacksonAdapter();
    }

    /**
     * Specifies the object id associated with a user assigned managed service identity
     * resource that should be used to retrieve the access token.
     *
     * @param objectId Object ID of the identity to use when authenticating to Azure AD.
     * @return MSICredentials
     */
    @Beta
    public MSICredentials withObjectId(String objectId) {
        this.objectId = objectId;
        this.clientId = null;
        this.identityId = null;
        return this;
    }

    /**
     * Specifies the application id (client id) associated with a user assigned managed service identity
     * resource that should be used to retrieve the access token.
     *
     * @param clientId application id (client id) of the identity to use when authenticating to Azure AD.
     * @return MSICredentials
     */
    @Beta
    public MSICredentials withClientId(String clientId) {
        this.clientId = clientId;
        this.objectId = null;
        this.identityId = null;
        return this;
    }

    /**
     * Specifies the ARM resource id of the user assigned managed service identity resource that
     * should be used to retrieve the access token.
     *
     * @param identityId the ARM resource id of the user assigned identity resource
     * @return MSICredentials
     */
    @Beta
    public MSICredentials withIdentityId(String identityId) {
        this.identityId = identityId;
        this.clientId = null;
        this.objectId = null;
        return this;
    }

    @Override
    public String getToken(String resource) throws IOException {
        URL url = new URL(String.format("http://localhost:%d/oauth2/token", this.msiPort));
        String postData = String.format("resource=%s", this.resource);
        if (this.objectId != null) {
            postData += String.format("&object_id=%s", this.objectId);
        } else if (this.clientId != null) {
            postData += String.format("&client_id=%s", this.clientId);
        } else if (this.identityId != null) {
            postData += String.format("&msi_res_id=%s", this.identityId);
        }
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            connection.setRequestProperty("Metadata", "true");
            connection.setRequestProperty("Content-Length", Integer.toString(postData.length()));
            connection.setDoOutput(true);

            connection.connect();

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(postData);
            wr.flush();

            InputStream stream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"), 100);
            String result = reader.readLine();

            MSIToken msiToken = adapter.deserialize(result, MSIToken.class);
            return msiToken.accessToken;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Type representing response from the local MSI token provider.
     */
    private static class MSIToken {
        /**
         * Token type "Bearer".
         */
        @JsonProperty(value = "token_type")
        private String tokenType;

        /**
         * Access token.
         */
        @JsonProperty(value = "access_token")
        private String accessToken;
    }
}
