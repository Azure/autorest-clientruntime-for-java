/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.arm.model.implementation;

import com.microsoft.azure.arm.model.Indexable;
import com.microsoft.azure.arm.model.Refreshable;

/**
 * The implementation for {@link Indexable} and {@link Refreshable}.
 *
 * @param <T> the fluent type of the resource
 */
public abstract class IndexableRefreshableImpl<T>
    extends IndexableImpl
    implements Refreshable<T> {

    protected IndexableRefreshableImpl() {
    }

    protected IndexableRefreshableImpl(String key) {
        super(key);
    }

    @Override
    public abstract T refresh();
}
