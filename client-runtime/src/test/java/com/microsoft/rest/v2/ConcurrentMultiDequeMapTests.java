package com.microsoft.rest.v2;

import com.microsoft.rest.v2.http.ConcurrentMultiDequeMap;
import com.microsoft.rest.v2.http.ConcurrentMultiHashMap;
import org.junit.Assert;
import org.junit.Test;

public class ConcurrentMultiDequeMapTests {

    @Test
    public void testConcurrentMultiHashMap() {
        ConcurrentMultiDequeMap<String, String> map = new ConcurrentMultiDequeMap<>();

        // Populate
        map.put("a", "0");
        map.put("a", "1");
        map.put("a", "2");
        map.put("a", "3");
        map.put("b", "10");
        map.put("b", "11");
        map.put("b", "12");
        map.put("c", "100");
        map.put("c", "101");

        Assert.assertEquals(9, map.size());

        // Poll by key
        Assert.assertEquals("10", map.poll("b"));
        Assert.assertEquals("0", map.poll("a"));

        // Poll by LRU
        Assert.assertEquals("100", map.pop());
        Assert.assertEquals("1", map.poll());
        Assert.assertEquals("2", map.poll());
        Assert.assertEquals("101", map.pop());

        // ContainsKey
        Assert.assertFalse(map.containsKey("c"));

        // Size
        Assert.assertEquals(3, map.size());
    }
}