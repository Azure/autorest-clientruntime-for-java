package com.microsoft.rest.v2.protocol;


import com.microsoft.rest.v2.http.HttpHeaders;

/**
 * Represents which encoding to use for serialization.
 */
public enum SerializerEncoding {
    /**
     * JavaScript Object Notation.
     */
    JSON,

    /**
     * Extensible Markup Language.
     */
    XML;

    public static SerializerEncoding fromHeaders(HttpHeaders headers) {
        String mimeContentType = headers.value("Content-Type");
        if (mimeContentType != null) {
            String[] parts = mimeContentType.split(";");
            if (parts[0].equalsIgnoreCase("application/xml") || parts[0].equalsIgnoreCase("text/xml")) {
                return XML;
            }
        }

        return JSON;
    }
}
