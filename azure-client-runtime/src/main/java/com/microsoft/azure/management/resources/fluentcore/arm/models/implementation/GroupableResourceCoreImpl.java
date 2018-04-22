/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.resources.fluentcore.arm.models.implementation;

import com.microsoft.azure.management.resources.fluentcore.arm.ResourceUtilsCore;
import com.microsoft.azure.management.resources.fluentcore.arm.implementation.ManagerBaseCore;
import com.microsoft.azure.management.resources.fluentcore.arm.models.GroupableResourceCore;
import com.microsoft.azure.management.resources.fluentcore.arm.models.Resource;

/**
 * The implementation for {@link GroupableResourceCore}.
 * (Internal use only)
 *
 * @param <FluentModelT> The fluent model type
 * @param <InnerModelT> Azure inner resource class type
 * @param <FluentModelImplT> the implementation type of the fluent model type
 * @param <ManagerT> the service manager type
 */
public abstract class GroupableResourceCoreImpl<
        FluentModelT extends Resource,
        InnerModelT extends com.microsoft.azure.Resource,
        FluentModelImplT extends GroupableResourceCoreImpl<FluentModelT, InnerModelT, FluentModelImplT, ManagerT>,
        ManagerT extends ManagerBaseCore>
        extends
            ResourceImpl<FluentModelT, InnerModelT, FluentModelImplT>
        implements
        GroupableResourceCore<ManagerT, InnerModelT> {

    protected final ManagerT myManager;
    private String groupName;

    protected GroupableResourceCoreImpl(
            String name,
            InnerModelT innerObject,
            ManagerT manager) {
        super(name, innerObject);
        this.myManager = manager;
    }

    /*******************************************
     * Getters.
     *******************************************/

    @Override
    public ManagerT manager() {
        return this.myManager;
    }

    @Override
    public String resourceGroupName() {
        if (this.groupName == null) {
            return ResourceUtilsCore.groupFromResourceId(this.id());
        } else {
            return this.groupName;
        }
    }

    /****************************************
     * withGroup implementations.
     ****************************************/

    /**
     * Associates the resources with an existing resource group.
     * @param groupName the name of an existing resource group to put this resource in.
     * @return the next stage of the definition
     */
    @SuppressWarnings("unchecked")
    public final FluentModelImplT withExistingResourceGroup(String groupName) {
        this.groupName = groupName;
        return (FluentModelImplT) this;
    }
}