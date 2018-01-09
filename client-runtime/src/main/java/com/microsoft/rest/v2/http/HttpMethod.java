package com.microsoft.rest.v2.http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.microsoft.rest.v2.ExpandableStringEnum;

import java.util.Collection;

/**
 * The different HTTP methods of a HttpRequest.
 */
public class HttpMethod extends ExpandableStringEnum<HttpMethod> {
    public static final HttpMethod GET = fromString("GET");
    public static final HttpMethod PUT = fromString("PUT");
    public static final HttpMethod POST = fromString("POST");
    public static final HttpMethod PATCH = fromString("PATCH");
    public static final HttpMethod DELETE = fromString("DELETE");
    public static final HttpMethod HEAD = fromString("HEAD");

    /**
     * Creates or finds a HttpMethod from its string representation.
     *
     * @param name a name to look for.
     * @return the corresponding HttpMethod.
     */
    @JsonCreator
    public static HttpMethod fromString(String name) {
        return fromString(name, HttpMethod.class);
    }

    /**
     * @return known HttpMethod values.
     */
    public static Collection<HttpMethod> values() {
        return values(HttpMethod.class);
    }
}
