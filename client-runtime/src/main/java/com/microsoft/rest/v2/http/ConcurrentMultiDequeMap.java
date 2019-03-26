/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.http;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread-safe multi map where the values for a certain key are FIFO organized.
 * @param <K> the key type
 * @param <V> the value type
 */
public class ConcurrentMultiDequeMap<K, V> {
    private final Map<K, ConcurrentLinkedDeque<V>> data;
    // Size is the total number of elements in all ConcurrentLinkedQueues in the Map.
    private final AtomicInteger size;
    // least recently updated keys
    private final LinkedList<K> lru;

    /**
     * Create a concurrent multi hash map.
     */
    public ConcurrentMultiDequeMap() {
        this.data = Collections.synchronizedMap(new ConcurrentHashMap<K, ConcurrentLinkedDeque<V>>(16, 0.75f));
        this.size = new AtomicInteger(0);
        this.lru = new LinkedList<>();
    }

    /**
     * Add a new key value pair to the multimap.
     *
     * @param key the key to put
     * @param value the value to put
     * @return the added value
     */
    public V put(K key, V value) {
        assert key != null;
        synchronized (size) {
            if (!data.containsKey(key)) {
                data.put(key, new ConcurrentLinkedDeque<V>());
                lru.addLast(key);
            } else {
                lru.remove(key);
                lru.addLast(key);
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
    public ConcurrentLinkedDeque<V> get(K key) {
        return data.get(key);
    }

    /**
     * Retrieves and removes one item from the multi map. The item is from
     * the least recently used key set.
     * @return the item removed from the map
     */
    public V poll() {
        K key;
        synchronized (size) {
            if (size.get() == 0) {
                return null;
            } else {
                key = lru.getFirst();
            }
        }
        return poll(key);
    }
    /**
     * Retrieves and removes one item from the multi map. The item is from
     * the most recently used key set.
     * @return the item removed from the map
     */
    public V pop() {
        K key;
        synchronized (size) {
            if (size.get() == 0) {
                return null;
            } else {
                key = lru.getLast();
            }
        }
        return pop(key);
    }

    /**
     * Retrieves the least recently used item in the deque for the given key.
     *
     * @param key the key to poll an item
     * @return the least recently used item for the key
     */
    public V poll(K key) {
        if (!data.containsKey(key)) {
            return null;
        } else {
            ConcurrentLinkedDeque<V> queue = data.get(key);
            V ret;
            synchronized (size) {
                if (queue == null || queue.isEmpty()) {
                    throw new NoSuchElementException("no items under key " + key);
                }
                size.decrementAndGet();
                ret = queue.poll();
                if (queue.isEmpty()) {
                    data.remove(key);
                    lru.remove(key);
                }
            }
            return ret;
        }
    }

    /**
     * Retrieves the most recently used item in the deque for the given key.
     *
     * @param key the key to poll an item
     * @return the most recently used item for the key
     */
    public V pop(K key) {
        if (!data.containsKey(key)) {
            return null;
        } else {
            ConcurrentLinkedDeque<V> queue = data.get(key);
            V ret;
            synchronized (size) {
                if (queue == null || queue.isEmpty()) {
                    throw new NoSuchElementException("no items under key " + key);
                }
                size.decrementAndGet();
                ret = queue.pop();
                if (queue.isEmpty()) {
                    data.remove(key);
                    lru.remove(key);
                }
            }
            return ret;
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
        Set<K> keys = new HashSet<>();
        for (K key : data.keySet()) {
            if (data.get(key).size() > 0) {
                keys.add(key);
            }
        }
        return keys;
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
        ConcurrentLinkedDeque<V> queue = data.get(key);
        boolean removed;
        synchronized (size) {
            removed = queue.remove(value);
            if (removed) {
                size.decrementAndGet();
            }
            if (queue.isEmpty()) {
                data.remove(key);
                lru.remove(key);
            }
        }
        return removed;
    }
}
