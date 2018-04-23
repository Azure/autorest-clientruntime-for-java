/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.arm.dag;

import com.microsoft.azure.arm.model.Creatable;
import com.microsoft.azure.arm.model.Executable;
import com.microsoft.azure.arm.model.Indexable;

/**
 * Represents a type when executed returns a bread from the store.
 */
public interface IBreadSlice extends Indexable, Executable<IBreadSlice> {
    IBreadSlice withAnotherSliceFromStore(Executable<IBreadSlice> breadFetcher);
    IBreadSlice withNewOrder(Creatable<IOrder> order);
}