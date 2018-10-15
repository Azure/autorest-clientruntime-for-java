/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.http;

import java.net.URI;

class ChannelDescriptions extends ConcurrentMultiHashMap<URI, ChannelDescription> {
    ChannelDescription removeAndGet(URI key, String descriptionId) {
        return removeAndGet(key, description -> description.id() == descriptionId);
    }
}
