/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.http;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread-safe multi map.
 */
public class ConcurrentMultiHashMap<K, V>{
    private ConcurrentHashMap<K, ConcurrentLinkedDeque<V>> data;
    private final AtomicInteger size;

    public ConcurrentMultiHashMap() {
        this.data = new ConcurrentHashMap<>();
        this.size = new AtomicInteger(0);
    }

    public V put(K key, V value) {
        synchronized (size) {
            if (!data.containsKey(key)) {
                data.put(key, new ConcurrentLinkedDeque<V>());
            }
            data.get(key).add(value);
            size.incrementAndGet();
            return value;
        }
    }

    public ConcurrentLinkedDeque<V> get(K key) {
        return data.get(key);
    }

    public V poll() {
        synchronized (size) {
            if (size.get() == 0) {
                return null;
            } else {
                Enumeration<K> keys = data.keys();
                K key = keys.nextElement();
                while (data.get(key).size() == 0) {
                    key = keys.nextElement();
                }
                return poll(key);
            }
        }
    }

    public V poll(K key) {
        if (!data.containsKey(key)) {
            return null;
        } else {
            synchronized (size) {
                size.decrementAndGet();
                return data.get(key).poll();
            }
        }
    }

    public int size() {
        return size.get();
    }

    public boolean containsKey(K key) {
        return data.containsKey(key) && data.get(key).size() > 0;
    }

    public Set<K> keys() {
        return data.keySet();
    }

    public Set<V> values() {
        Set<V> values = new HashSet<>();
        for (K k : keys()) {
            values.addAll(data.get(k));
        }
        return values;
    }

    public boolean remove(K key, V value) {
        if (!data.containsKey(key)) {
            return false;
        }
        synchronized (size) {
            size.decrementAndGet();
            return data.get(key).remove(value);
        }
    }
}
