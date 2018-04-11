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
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Managed Service Identity token based credentials for use with a REST Service Client.
 */
@Beta
public class MSICredentials extends AzureTokenCredentials {
    //
    private final List<Integer> retrySlots = new ArrayList<>(Arrays.asList(new Integer[] {1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765}));
    private final int maxRetry = retrySlots.size();
    private final Lock lock = new ReentrantLock();
    private final ConcurrentHashMap<String, MSIToken> cache = new ConcurrentHashMap<>();
    //
    private final String resource;
    private int msiPort = 50342;
    private final MSITokenSource tokenSource;
    private final AzureJacksonAdapter adapter = new AzureJacksonAdapter();
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
        super(environment, null /** retrieving MSI token does not require tenant **/);
        this.resource = environment.resourceManagerEndpoint();
        this.tokenSource = MSITokenSource.IMDS_ENDPOINT;
    }

    /**
     * Initializes a new instance of the MSICredentials.
     *
     * @param environment the Azure environment to use
     * @param msiPort the local port to retrieve token from
     * @deprecated use {@link #MSICredentials()} or {@link #MSICredentials(AzureEnvironment)} instead.
     */
    @Deprecated()
    public MSICredentials(AzureEnvironment environment, int msiPort) {
        super(environment, null /** retrieving MSI token does not require tenant **/);
        this.resource = environment.resourceManagerEndpoint();
        this.msiPort = msiPort;
        this.tokenSource = MSITokenSource.MSI_EXTENSION;
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
        if (this.tokenSource == MSITokenSource.MSI_EXTENSION) {
            return this.getTokenFromMSIExtension();
        } else {
            return this.getTokenFromIMDSEndpoint();
        }
    }

    private String getTokenFromMSIExtension() throws IOException {
        URL url = new URL(String.format("http://localhost:%d/oauth2/token", this.msiPort));
        String postData = String.format("resource=%s", this.resource);
        if (this.objectId != null) {
            postData += String.format("&object_id=%s", this.objectId);
        } else if (this.clientId != null) {
            postData += String.format("&client_id=%s", this.clientId);
        } else if (this.identityId != null) {
            postData += String.format("&msi_res_id=%s", this.identityId);
        } else {
            throw new IllegalArgumentException("objectId, clientId or identityId must be set");
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

    private String getTokenFromIMDSEndpoint() {
        MSIToken token = cache.get(resource);
        if (token != null && !token.isExpired()) {
            return token.accessToken();
        }
        lock.lock();
        try {
            token = cache.get(resource);
            if (token != null && !token.isExpired()) {
                return token.accessToken();
            }
            try {
                token = retrieveTokenFromIDMSWithRetry();
                if (token != null) {
                    cache.put(resource, token);
                }
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
            return token.accessToken();
        } finally {
            lock.unlock();
        }
    }

    private MSIToken retrieveTokenFromIDMSWithRetry() throws IOException {
        StringBuilder payload = new StringBuilder();
        //
        try {
            payload.append("api-version");
            payload.append("=");
            payload.append(URLEncoder.encode("2018-02-01", "UTF-8"));
            payload.append("&");
            payload.append("resource");
            payload.append("=");
            payload.append(URLEncoder.encode(this.resource, "UTF-8"));
            payload.append("&");
            if (this.objectId != null) {
                payload.append("object_id");
                payload.append("=");
                payload.append(URLEncoder.encode(this.objectId, "UTF-8"));
            } else if (this.clientId != null) {
                payload.append("client_id");
                payload.append("=");
                payload.append(URLEncoder.encode(this.clientId, "UTF-8"));
            } else if (this.identityId != null) {
                payload.append("msi_res_id");
                payload.append("=");
                payload.append(URLEncoder.encode(this.identityId, "UTF-8"));
            } else {
                throw new IllegalArgumentException("objectId, clientId or identityId must be set");
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        int retry = 1;
        while (retry <= maxRetry) {
            URL url = new URL(String.format("http://169.254.169.254/metadata/identity/oauth2/token?%s", payload.toString()));
            //
            HttpURLConnection connection = null;
            //
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Metadata", "true");
                connection.connect();
                InputStream stream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"), 100);
                String result = reader.readLine();
                return adapter.deserialize(result, MSIToken.class);
            } catch (Exception exception) {
                int responseCode = connection.getResponseCode();
                if (responseCode == 429 || responseCode == 404 || (responseCode >= 500 && responseCode <= 599)) {
                    int retryTimeout = retrySlots.get(new Random().nextInt(retry)) * 1000;
                    sleep(retryTimeout);
                    retry++;
                } else {
                    throw new RuntimeException(exception);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        //
        if (retry > maxRetry) {
            throw new RuntimeException(String.format("MSI: Failed to acquire tokens after retrying %s times", maxRetry));
        }
        return null;
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * The source of MSI token.
     */
    private enum MSITokenSource {
        /**
         * Indicate that token should be retrieved from MSI extension installed in the VM.
         */
        MSI_EXTENSION,
        /**
         * Indicate that token should be retrieved from IMDS service.
         */
        IMDS_ENDPOINT
    }
}
