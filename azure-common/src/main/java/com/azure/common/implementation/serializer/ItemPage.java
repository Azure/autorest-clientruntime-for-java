package com.azure.common.implementation.serializer;

import com.azure.common.http.rest.Page;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

class ItemPage<T> implements Page<T> {
    @JsonProperty()
    private List<T> items;

    @JsonProperty("nextLink")
    private String nextLink;

    @Override
    public List<T> items() {
        return items;
    }

    @Override
    public String nextLink() {
        return nextLink;
    }
}
