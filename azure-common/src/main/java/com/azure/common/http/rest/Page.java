package com.azure.common.http.rest;

import java.util.List;

public interface Page<T> {
    List<T> items();

    String nextLink();
}
