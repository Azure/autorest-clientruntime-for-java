/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.protocol;

import java.lang.reflect.Type;

/**
 * A factory interface that can be used to create Types that can be used with a SerializerAdapter.
 */
public interface TypeFactory {
    /**
     * Create a SerializerAdapter-specific Type for the provided Type.
     * @param type The type.
     * @return The SerializerAdapter-specific Type.
     */
    Type create(Type type);
}
