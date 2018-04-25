/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.arm.resources.models;

import com.microsoft.azure.arm.model.HasInner;
import com.microsoft.azure.management.apigeneration.Fluent;

/**
 * Base interface for resources in resource groups.
 * @param <TManager> the manager object type representing the service
 * @param <InnerT> the wrapped, inner, auto-generated implementation object type
 */
@Fluent()
public interface GroupableResourceCore<TManager, InnerT> extends
        Resource,
        HasResourceGroup,
        HasManager<TManager>,
        HasInner<InnerT> {

    /**
     * Grouping of all the definition stages.
     */
    interface DefinitionStages {
        /**
         * A resource definition allowing a resource group to be selected.
         * <p>
         * Region of the groupable resource will be used for new resource group
         *
         * @param <T> the next stage of the definition
         */
        interface WithGroup<T> extends
                WithExistingResourceGroup<T> {
        }

        /**
         * A resource definition allowing an existing resource group to be selected.
         *
         * @param <T> the next stage of the definition
         */
        interface WithExistingResourceGroup<T> {
            /**
             * Associates the resource with an existing resource group.
             * @param groupName the name of an existing resource group to put this resource in.
             * @return the next stage of the definition
             */
            T withExistingResourceGroup(String groupName);
        }
    }
}
