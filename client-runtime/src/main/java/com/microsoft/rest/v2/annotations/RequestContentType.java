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
 * Defines the content type (MIME type) for the request body.
 *
 * If the Content-Type header present in {@link Headers} annotation then this annotation override it because this annotation is more specialized.
 * If the Content-Type header is passed as parameter via {@link HeaderParam} annotation then {@link HeaderParam} takes precedence over this annotation,
 * because that is what user selected during runtime.
 * The value of this annotation overrides "Content-Type" header added in any interceptors, and "Content-Type" header added by the HTTP itself.
 *
 * The precedence is @Header("Content-Type") [dynamic] > @RequestContentType(value) [static] > @Headers("Content-Type: ..") [static]
 *
 * Once the "Content-Type" is derived, value will be used to choose the converter to encode the user provided POJO object to send it over wire.
 * Finally derived "Content-Type" become the value of "Content-Type" header in the request.
 */
@Target({ElementType.METHOD})            // The context in which annotation is applicable i.e. this annotation (RequestContentType) can be applied only to methods
@Retention(RetentionPolicy.RUNTIME)      // Record this annotation in the class file and make it available during runtime.
public @interface RequestContentType {
    String value();
}

