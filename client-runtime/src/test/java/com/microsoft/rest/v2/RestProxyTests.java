/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2;

import org.junit.Test;

import java.security.InvalidParameterException;

public class RestProxyTests {

    @Test(expected = InvalidParameterException.class)
    public void validateHostAnnotationValueWithNull() throws Exception {
        RestProxy.validateHostAnnotationValue(null);
    }

    @Test(expected = InvalidParameterException.class)
    public void validateHostAnnotationValueWithEmpty() throws Exception {
        RestProxy.validateHostAnnotationValue("");
    }

    @Test(expected = InvalidParameterException.class)
    public void validateHostAnnotationValueWithNoSchemeHostSeparator() throws Exception {
        RestProxy.validateHostAnnotationValue("httpmanagement.azure.com");
    }

    @Test(expected = InvalidParameterException.class)
    public void validateHostAnnotationValueWithNoHostAfterSeparator() throws Exception {
        RestProxy.validateHostAnnotationValue("http://");
    }

    @Test(expected = InvalidParameterException.class)
    public void validateHostAnnotationValueWithNoSchemeBeforeSeparator() throws Exception {
        RestProxy.validateHostAnnotationValue("://management.azure.com");
    }

    @Test
    public void validateHostAnnotationValue() {
        RestProxy.validateHostAnnotationValue("https://management.azure.com");
    }
}
