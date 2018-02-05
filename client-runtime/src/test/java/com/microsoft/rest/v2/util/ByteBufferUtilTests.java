package com.microsoft.rest.v2.util;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class ByteBufferUtilTests {
    @Test
    public void toByteArrayWithNullByteBuffer()
    {
        assertNull(ByteBufferUtil.toByteArray(null));
    }

    @Test
    public void toByteArrayWithEmptyByteBuffer()
    {
        assertArrayEquals(new byte[0], ByteBufferUtil.toByteArray(ByteBuffer.wrap(new byte[0])));
    }

    @Test
    public void toByteArrayWithNonEmptyByteBuffer()
    {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[] { 0, 1, 2, 3, 4 });
        assertEquals(5, byteBuffer.remaining());
        assertArrayEquals(new byte[] { 0, 1, 2, 3, 4 }, ByteBufferUtil.toByteArray(byteBuffer));
        assertEquals(0, byteBuffer.remaining());
    }
}
