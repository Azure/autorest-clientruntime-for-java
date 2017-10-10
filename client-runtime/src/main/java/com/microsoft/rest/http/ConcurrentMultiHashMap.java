/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.http;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread-safe multi map where the values for a certain key are FIFO organized.
 * @param <K> the key type
 * @param <V> the value type
 */
public class ConcurrentMultiHashMap<K, V> {
    private ConcurrentHashMap<K, ConcurrentLinkedQueue<V>> data;
    private final AtomicInteger size;

    /**
     * Create a concurrent multi hash map.
     */
    public ConcurrentMultiHashMap() {
        this.data = new ConcurrentHashMap<>();
        this.size = new AtomicInteger(0);
    }

    /**
     * Add a new key value pair to the multimap.
     *
     * @param key the key to put
     * @param value the value to put
     * @return the added value
     */
    public V put(K key, V value) {
        synchronized (size) {
            if (!data.containsKey(key)) {
                data.put(key, new ConcurrentLinkedQueue<V>());
            }
            data.get(key).add(value);
            size.incrementAndGet();
            return value;
        }
    }

    /**
     * Returns the queue associated with the given key.
     *
     * @param key the key to query
     * @return the queue associated with the key
     */
    public ConcurrentLinkedQueue<V> get(K key) {
        return data.get(key);
    }

    /**
     * Retrieves one item from the multi map.
     * @return the item immediately available in the map
     */
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

    /**
     * Retrieves the least recently used item in the queue for the given key.
     *
     * @param key the key to poll an item
     * @return the least recently used item for the key
     */
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

    /**
     * @return the size of the multimap.
     */
    public int size() {
        return size.get();
    }

    /**
     * Checks if there are values associated with a key in the multimap.
     *
     * @param key the key to check
     * @return true if there are values associated
     */
    public boolean containsKey(K key) {
        return data.containsKey(key) && data.get(key).size() > 0;
    }

    /**
     * @return the set of keys with which there are values associated
     */
    public Set<K> keys() {
        return Sets.filter(data.keySet(), new Predicate<K>() {
            @Override
            public boolean apply(K input) {
                return data.get(input).size() > 0;
            }
        });
    }

    /**
     * @return the set of all values for all keys in the multimap.
     */
    public Set<V> values() {
        Set<V> values = new HashSet<>();
        for (K k : keys()) {
            values.addAll(data.get(k));
        }
        return values;
    }

    /**
     * Removes a key value pair in the multimap. If there's no such key value
     * pair then this returns false. Otherwise this method removes it and
     * returns true.
     *
     * @param key the key to remove
     * @param value the value to remove
     * @return true if an item is removed
     */
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
