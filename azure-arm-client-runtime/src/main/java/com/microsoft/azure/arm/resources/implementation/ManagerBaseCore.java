/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.arm.resources.implementation;

/**
 * Base class for Azure resource managers.
 */
public abstract class ManagerBaseCore {

    private final String subscriptionId;

    protected ManagerBaseCore(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    /**
     * @return the ID of the subscription the manager is working with
     */
    public String subscriptionId() {
        return this.subscriptionId;
    }
}
