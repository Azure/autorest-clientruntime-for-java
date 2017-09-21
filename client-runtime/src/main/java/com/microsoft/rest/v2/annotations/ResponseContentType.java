/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the content type (MIME type) of the response body.
 *
 * If the Content-Type header present in the response then that takes precedence over this annotation.
 *
 * The precedence is Response.Header("Content-Type") > @ResponseContentType(value)
 *
 * Once the "Content-Type" is derived, value will be used to choose the converter to decode response body bytes to POJO object
 */
@Target({ElementType.METHOD})            // The context in which annotation is applicable i.e. this annotation (ResponseContentType) can be applied only to methods
@Retention(RetentionPolicy.RUNTIME)      // Record this annotation in the class file and make it available during runtime.
public @interface ResponseContentType {
    String value();
}

