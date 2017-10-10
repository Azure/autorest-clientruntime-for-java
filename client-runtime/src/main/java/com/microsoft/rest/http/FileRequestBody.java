/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.http;

/**
 * A HTTP request body that contains a chunk of a file.
 */
public class FileRequestBody implements HttpRequestBody {
    private final FileSegment fileSegment;

    /**
     * Create a new FileHttpRequestBody with the provided file.
     *
     * @param fileSegment the segment of file as the request body
     */
    public FileRequestBody(FileSegment fileSegment) {
        this.fileSegment = fileSegment;
    }

    @Override
    public int contentLength() {
        return fileSegment.length();
    }

    /**
     * @return the lazy loaded content of the request, in the format of a file segment.
     */
    public FileSegment content() {
        return fileSegment;
    }
}
