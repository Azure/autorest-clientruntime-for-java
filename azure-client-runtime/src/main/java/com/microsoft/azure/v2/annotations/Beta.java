/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.v2.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Beta annotation.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ TYPE, METHOD, PARAMETER, CONSTRUCTOR })
@Inherited
/**
 * Indicates functionality that is in preview and as such is subject to change in non-backwards compatible ways in future releases,
 * including removal, regardless of any compatibility expectations set by the containing library version.
 */
public @interface Beta {
    /**
     * @return the warning message.
     */
    String warningText() default "This functionality is in preview and as such is subject to change in non-backwards compatible ways in future releases, including removal, regardless of any compatibility expectations set by the containing library version.";

    /**
     * @return the version number when the annotated API was first introduced to the library as in Beta.
     */
    String value() default "";
}
