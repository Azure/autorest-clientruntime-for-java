/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.protocol.SerializerAdapter;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * An instance of this class defines polling status of a long running operation.
 *
 * @param <T> the type of the resource the operation returns.
 */
public class PollingState<T> {
    /** The HTTP method used to initiate the long running operation. **/
    private String initialHttpMethod;
    /** The polling status. */
    private String status;
    /** The link in 'Azure-AsyncOperation' header. */
    private String azureAsyncOperationHeaderLink;
    /** The link in 'Location' Header. */
    private String locationHeaderLink;
    /** The default timeout interval between two polling operations. */
    private final int defaultRetryTimeout;
    /** The timeout interval between two polling operation. **/
    private int retryTimeout;
    /** The resource uri on which PUT or PATCH operation is applied. **/
    private String putOrPatchResourceUri;

    // Non-serializable properties
    //
    /** The Retrofit response object. */
    @JsonIgnore
    private Response<ResponseBody> response;
    /** The response resource object. */
    @JsonIgnore
    private T resource;
    /** The type of the response resource object. */
    @JsonIgnore
    private Type resourceType;
    /** The error during the polling operations. */
    @JsonIgnore
    private CloudError error;
    /** The adapter for a custom serializer. */
    @JsonIgnore
    private SerializerAdapter<?> serializerAdapter;

    /**
     * Initializes an instance of {@link PollingState}.
     *
     * @param response the response from Retrofit REST call that initiate the long running operation.
     * @param defaultRetryTimeout the long running operation retry timeout.
     * @param resourceType the type of the resource the long running operation returns
     * @param serializerAdapter the adapter for the Jackson object mapper
     * @throws IOException thrown by deserialization
     */
    PollingState(Response<ResponseBody> response, int defaultRetryTimeout, Type resourceType, SerializerAdapter<?> serializerAdapter) throws IOException {
        this.initialHttpMethod = response.raw().request().method();
        this.defaultRetryTimeout = defaultRetryTimeout;
        this.withResponse(response);
        this.resourceType = resourceType;
        this.serializerAdapter = serializerAdapter;

        String responseContent = null;
        PollingResource resource = null;
        if (response.body() != null) {
            responseContent = response.body().string();
            response.body().close();
        }
        if (responseContent != null && !responseContent.isEmpty()) {
            this.resource = serializerAdapter.deserialize(responseContent, resourceType);
            resource = serializerAdapter.deserialize(responseContent, PollingResource.class);
        }
        if (resource != null && resource.properties != null
                && resource.properties.provisioningState != null) {
            withStatus(resource.properties.provisioningState);
        } else {
            switch (this.response.code()) {
                case 202:
                    withStatus(AzureAsyncOperation.IN_PROGRESS_STATUS);
                    break;
                case 204:
                case 201:
                case 200:
                    withStatus(AzureAsyncOperation.SUCCESS_STATUS);
                    break;
                default:
                    withStatus(AzureAsyncOperation.FAILED_STATUS);
            }
        }
    }

    /**
     * Gets the resource.
     *
     * @return the resource.
     */
    public T resource() {
        return resource;
    }

    /**
     * Gets the operation response.
     *
     * @return the operation response.
     */
    public Response<ResponseBody> response() {
        return this.response;
    }

    /**
     * Gets the polling status.
     *
     * @return the polling status.
     */
    public String status() {
        return status;
    }

    /**
     * Gets the value captured from Azure-AsyncOperation header.
     *
     * @return the link in the header.
     */
    public String azureAsyncOperationHeaderLink() {
        return azureAsyncOperationHeaderLink;
    }

    /**
     * Gets the value captured from Location header.
     *
     * @return the link in the header.
     */
    public String locationHeaderLink() {
        return locationHeaderLink;
    }

    /**
     * Updates the polling state from a PUT or PATCH operation.
     *
     * @param response the response from Retrofit REST call
     * @throws CloudException thrown if the response is invalid
     * @throws IOException thrown by deserialization
     */
    void updateFromResponseOnPutPatch(Response<ResponseBody> response) throws CloudException, IOException {
        String responseContent = null;
        if (response.body() != null) {
            responseContent = response.body().string();
            response.body().close();
        }

        if (responseContent == null || responseContent.isEmpty()) {
            throw new CloudException("polling response does not contain a valid body", response);
        }

        PollingResource resource = serializerAdapter.deserialize(responseContent, PollingResource.class);
        if (resource != null && resource.properties != null && resource.properties.provisioningState != null) {
            this.withStatus(resource.properties.provisioningState);
        } else {
            this.withStatus(AzureAsyncOperation.SUCCESS_STATUS);
        }

        CloudError error = new CloudError();
        this.withErrorBody(error);
        error.withCode(this.status());
        error.withMessage("Long running operation failed");
        this.withResponse(response);
        this.withResource(serializerAdapter.<T>deserialize(responseContent, resourceType));
    }

    /**
     * Updates the polling state from a DELETE or POST operation.
     *
     * @param response the response from Retrofit REST call
     * @throws IOException thrown by deserialization
     */

    void updateFromResponseOnDeletePost(Response<ResponseBody> response) throws IOException {
        this.withResponse(response);
        String responseContent = null;
        if (response.body() != null) {
            responseContent = response.body().string();
            response.body().close();
        }
        this.withResource(serializerAdapter.<T>deserialize(responseContent, resourceType));
        withStatus(AzureAsyncOperation.SUCCESS_STATUS);
    }

    /**
     * Gets long running operation delay in milliseconds.
     *
     * @return the delay in milliseconds.
     */
    int delayInMilliseconds() {
        if (this.retryTimeout >= 0) {
            return this.retryTimeout;
        }
        if (this.defaultRetryTimeout >= 0) {
            return this.defaultRetryTimeout * 1000;
        }
        return AzureAsyncOperation.DEFAULT_DELAY * 1000;
    }

    /**
     * @return the uri of the resource on which the LRO PUT or PATCH applied.
     */
    String putOrPatchResourceUri() {
        return this.putOrPatchResourceUri;
    }

    /**
     * @return true if the status this state hold represents terminal status.
     */
    boolean isStatusTerminal() {
        for (String terminalStatus : AzureAsyncOperation.terminalStatuses()) {
            if (terminalStatus.equalsIgnoreCase(this.status())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the status this state hold is represents failed status.
     */
    boolean isStatusFailed() {
        for (String failedStatus : AzureAsyncOperation.failedStatuses()) {
            if (failedStatus.equalsIgnoreCase(this.status())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the status this state represents is succeeded status.
     */
    boolean isStatusSucceeded() {
        return AzureAsyncOperation.SUCCESS_STATUS.equalsIgnoreCase(this.status());
    }

    /**
     * Sets the polling status.
     *
     * @param status the polling status.
     * @throws IllegalArgumentException thrown if status is null.
     */
    PollingState<T> withStatus(String status) throws IllegalArgumentException {
        if (status == null) {
            throw new IllegalArgumentException("Status is null.");
        }
        this.status = status;
        return this;
    }

    /**
     * Sets the last operation response.
     *
     * @param response the last operation response.
     */
    PollingState<T> withResponse(Response<ResponseBody> response) {
        this.response = response;
        withPollingUrlFromResponse(response);
        withPollingRetryTimeoutFromResponse(response);
        return this;
    }

    PollingState<T> withPollingUrlFromResponse(Response<ResponseBody> response) {
        if (response != null) {
            String asyncHeader = response.headers().get("Azure-AsyncOperation");
            String locationHeader = response.headers().get("Location");
            if (asyncHeader != null) {
                this.azureAsyncOperationHeaderLink = asyncHeader;
            }
            if (locationHeader != null) {
                this.locationHeaderLink = locationHeader;
            }
        }
        return this;
    }

    PollingState<T> withPollingRetryTimeoutFromResponse(Response<ResponseBody> response) {
        if (this.response != null && response.headers().get("Retry-After") != null) {
            retryTimeout = Integer.parseInt(response.headers().get("Retry-After")) * 1000;
            return this;
        }
        this.retryTimeout = -1;
        return this;
    }

    PollingState<T> withPutOrPatchResourceUri(final String uri) {
        this.putOrPatchResourceUri = uri;
        return this;
    }

    /**
     * Sets the resource.
     *
     * @param resource the resource.
     */
    PollingState<T> withResource(T resource) {
        this.resource = resource;
        return this;
    }

    /**
     * @return the resource type
     */
    Type resourceType() {
        return resourceType;
    }

    /**
     * Sets resource type.
     *
     * param resourceType the resource type
     */
    PollingState<T> withResourceType(Type resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    /**
     * Gets {@link CloudError} from current instance.
     *
     * @return the cloud error.
     */
    CloudError errorBody() {
        return error;
    }

    /**
     * Sets {@link CloudError} from current instance.
     *
     * @param error the cloud error.
     */
    PollingState<T> withErrorBody(CloudError error) {
        this.error = error;
        return this;
    }

    /**
     * Sets the serializer adapter.
     *
     * @param serializerAdapter the serializer adapter.
     */
    PollingState<T> withSerializerAdapter(SerializerAdapter<?> serializerAdapter) {
        this.serializerAdapter = serializerAdapter;
        return this;
    }

    /**
     * @return the http method used to initiate the long running operation.
     */
    String initialHttpMethod() {
        return this.initialHttpMethod;
    }

    /**
     * If status is in failed state then throw CloudException.
     */
    void throwCloudExceptionIfInFailedState() {
        if (this.isStatusFailed()) {
            if (this.errorBody() != null) {
                throw new CloudException("Async operation failed with provisioning state: " + this.status(), this.response(), this.errorBody());
            } else {
                throw new CloudException("Async operation failed with provisioning state: " + this.status(), this.response());
            }
        }
    }

    /**
     * An instance of this class describes the status of a long running operation
     * and is returned from server each time.
     */
    private static class PollingResource {
        /** Inner properties object. */
        @JsonProperty(value = "properties")
        private Properties properties;

        /**
         * Inner properties class.
         */
        private static class Properties {
            /** The provisioning state of the resource. */
            @JsonProperty(value = "provisioningState")
            private String provisioningState;
        }
    }
}