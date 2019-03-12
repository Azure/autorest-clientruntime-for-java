// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.rest.v3.http.rest;

import java.io.Closeable;
import java.util.List;

/**
 * Response of a REST API that returns page.
 *
 * @param <T> the type items in the page
 */
public interface RestPagedResponse<T> extends RestResponse<List<T>>, Closeable {
    /**
     * Gets the items in the page.
     *
     * @return the items
     */
    List<T> items();

    /**
     * Get the link to retrieve RestPagedResponse containing next page.
     *
     * @return the next page link
     */
    String nextLink();

    default List<T> body() {
        return items();
    }
}
