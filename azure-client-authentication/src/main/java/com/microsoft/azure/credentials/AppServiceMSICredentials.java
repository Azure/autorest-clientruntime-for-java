/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.credentials;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.management.apigeneration.Beta;
import com.microsoft.azure.serializer.AzureJacksonAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Managed Service Identity token based credentials for use with a REST Service Client.
 */
@Beta
public class AppServiceMSICredentials extends AzureTokenCredentials {
    private final String endpoint;
    private final String secret;
    private final AzureJacksonAdapter adapter;

    public AppServiceMSICredentials(AzureEnvironment environment) {
        this(environment, System.getenv("MSI_ENDPOINT"), System.getenv("MSI_SECRET"));
    }

    public AppServiceMSICredentials(AzureEnvironment environment, String endpoint, String secret) {
        super(environment, null);
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint == null");
        }
        if (secret == null) {
            throw new IllegalArgumentException("secret == null");
        }
        this.endpoint = endpoint;
        this.secret = secret;
        this.adapter = new AzureJacksonAdapter();
    }

    @Override
    public String getToken(String resource) throws IOException {
        URL url = new URL(String.format("%s?api-version=2017-09-01&resource=%s", this.endpoint, resource));
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("secret", this.secret);

            connection.connect();

            InputStream stream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"), 100);
            String result = reader.readLine();

            MSIToken msiToken = adapter.deserialize(result, MSIToken.class);
            return msiToken.accessToken();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
