/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure;

import java.io.Serializable;

/**
 * The SubResource model.
 */
public class SubResource implements Serializable {
    private static final long serialVersionUID = 1189719074576693L;

    /**
     * Resource Id.
     */
    private String id;

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public String id() {
        return this.id;
    }

    /**
     * Set the id value.
     *
     * @param id the id value to set
     * @return the sub resource itself
     */
    public SubResource withId(String id) {
        this.id = id;
        return this;
    }
}