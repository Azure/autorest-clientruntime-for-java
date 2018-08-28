/*
 * Copyright (C) 2009 The Guava Authors
 * Portions Copyright (c) Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.microsoft.rest.v2.util.escapers;

public final class UrlEscapers {

    private static final String UNRESERVED_SYMBOLS = "-._~";
    private static final String SUB_DELIMS = "!$&'()*+,;=";

    public static final PercentEscaper PATH_ESCAPER = new PercentEscaper(UNRESERVED_SYMBOLS + SUB_DELIMS + ":@", false);
    public static final PercentEscaper QUERY_ESCAPER = new PercentEscaper(UNRESERVED_SYMBOLS + "g/?", false);
    public static final PercentEscaper FORM_ESCAPER = new PercentEscaper(UNRESERVED_SYMBOLS, true);

}
