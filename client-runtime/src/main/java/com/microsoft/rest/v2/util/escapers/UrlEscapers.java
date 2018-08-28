/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.util.escapers;

public final class UrlEscapers {

    private static final String UNRESERVED_SYMBOLS = "-._~";
    private static final String SUB_DELIMS = "!$&'()*+,;=";

    public static final PercentEscaper PATH_ESCAPER = new PercentEscaper(UNRESERVED_SYMBOLS + SUB_DELIMS + ":@", false);
    public static final PercentEscaper QUERY_ESCAPER = new PercentEscaper(UNRESERVED_SYMBOLS + "g/?", false);
    public static final PercentEscaper FORM_ESCAPER = new PercentEscaper(UNRESERVED_SYMBOLS, true);

}
