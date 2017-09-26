/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

/**
 * A HTTP request body that contains a chunk of a file.
 */
public class FileHttpRequestBody implements HttpRequestBody {
    private final File file;
    private final long offset;
    private final int length;

    /**
     * Create a new FileHttpRequestBody with the provided file.
     *
     * @param file the file to send the request
     * @param offset the starting byte index in the file
     * @param length the length of the bytes to send
     */
    public FileHttpRequestBody(File file, long offset, int length) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File is null or does not exist.");
        }
        try {
            if (offset + length > file.length()) {
                throw new IndexOutOfBoundsException("Position " + offset + " + length " + length + " but file size " + fileChannel.size());
            }
            this.file = file;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read from file.", e);
        }
        this.offset = offset;
        this.length = length;
    }

    @Override
    public int contentLength() {
        return length;
    }

    @Override
    public InputStream createInputStream() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File disappeared!", e);
        }
        Channels.newInputStream()
    }
}
