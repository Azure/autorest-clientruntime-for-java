/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

/**
 * A HTTP request body that contains a chunk of a file.
 */
public class FileHttpRequestBody implements HttpRequestBody {
    private final FileChannel fileChannel;
    private final long offset;
    private final int length;
    private final ByteBufAllocator allocator;

    /**
     * Create a new FileHttpRequestBody with the provided file.
     *
     * @param fileChannel the file to send in the request
     * @param offset the starting byte index in the file
     * @param length the length of the bytes to send
     */
    public FileHttpRequestBody(FileChannel fileChannel, long offset, int length) {
        this(fileChannel, offset, length, ByteBufAllocator.DEFAULT);
    }

    /**
     * Create a new FileHttpRequestBody with the provided file.
     *
     * @param fileChannel the file to send in the request
     * @param offset the starting byte index in the file
     * @param length the length of the bytes to send
     * @param allocator the allocator for allocating a {@link ByteBuf}
     */
    public FileHttpRequestBody(FileChannel fileChannel, long offset, int length, ByteBufAllocator allocator) {
        if (fileChannel == null || !fileChannel.isOpen()) {
            throw new IllegalArgumentException("File channel is null or closed.");
        }
        try {
            if (offset + length > fileChannel.size()) {
                throw new IndexOutOfBoundsException("Position " + offset + " + length " + length + " but file size " + fileChannel.size());
            }
            this.fileChannel = fileChannel;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read from file.", e);
        }
        this.offset = offset;
        this.length = length;
        this.allocator = allocator;
    }

    @Override
    public int contentLength() {
        return length;
    }

    @Override
    public InputStream createInputStream() {
        ByteBuf direct = allocator.buffer(length, length);
        try {
            direct.writeBytes(fileChannel, offset, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ByteBufInputStream(direct);
    }
}
