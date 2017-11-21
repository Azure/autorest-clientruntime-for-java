/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.http;

/**
 * A builder class that is used to create URLs.
 */
public class UrlBuilder {
    private String scheme;
    private String host;
    private Integer port;
    private String path;
    private String query;

    /**
     * Set the scheme/protocol that will be used to build the final URL.
     * @param scheme The scheme/protocol that will be used to build the final URL.
     * @return This UrlBuilder so that multiple setters can be chained together.
     */
    public UrlBuilder withScheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    /**
     * Get the scheme/protocol that has been assigned to this UrlBuilder.
     * @return the scheme/protocol that has been assigned to this UrlBuilder.
     */
    public String scheme() {
        return scheme;
    }

    /**
     * Set the host that will be used to build the final URL.
     * @param host The host that will be used to build the final URL.
     * @return This UrlBuilder so that multiple setters can be chained together.
     */
    public UrlBuilder withHost(String host) {
        if (host != null && host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        this.host = host;
        return this;
    }

    /**
     * Get the host that has been assigned to this UrlBuilder.
     * @return the host that has been assigned to this UrlBuilder.
     */
    public String host() {
        return host;
    }

    /**
     * Set the port that will be used to build the final URL.
     * @param port The port that will be used to build the final URL.
     * @return This UrlBuilder so that multiple setters can be chained together.
     */
    public UrlBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Get the port that has been assigned to this UrlBuilder.
     * @return the port that has been assigned to this UrlBuilder.
     */
    public Integer port() {
        return port;
    }

    /**
     * Set the path that will be used to build the final URL.
     * @param path The path that will be used to build the final URL.
     * @return This UrlBuilder so that multiple setters can be chained together.
     */
    public UrlBuilder withPath(String path) {
        if (path != null) {
            String[] parts = path.split("\\?");
            this.path = parts[0];
            if (parts.length > 1) {
                String[] queryPairs = parts[1].split("&");
                for (String queryPair : queryPairs) {
                    String[] nameAndValue = queryPair.split("=");
                    if (nameAndValue.length != 2) {
                        throw new IllegalArgumentException("Path contained malformed query: " + path);
                    }

                    withQueryParameter(nameAndValue[0], nameAndValue[1]);
                }
            }
        }
        return this;
    }

    /**
     * Get the path that has been assigned to this UrlBuilder.
     * @return the path that has been assigned to this UrlBuilder.
     */
    public String path() {
        return path;
    }

    /**
     * Add the provided query parameter name and encoded value to query string for the final URL.
     * @param queryParameterName The name of the query parameter.
     * @param queryParameterEncodedValue The encoded value of the query parameter.
     * @return The provided query parameter name and encoded value to query string for the final
     * URL.
     */
    public UrlBuilder withQueryParameter(String queryParameterName, String queryParameterEncodedValue) {
        if (query == null) {
            query = "?";
        }
        else {
            query += "&";
        }
        query += queryParameterName + "=" + queryParameterEncodedValue;
        return this;
    }

    /**
     * Get the query that has been assigned to this UrlBuilder.
     * @return the query that has been assigned to this UrlBuilder.
     */
    public String query() {
        return query;
    }

    /**
     * Get the string representation of the URL that is being built.
     * @return The string representation of the URL that is being built.
     */
    public String toString() {
        final StringBuilder result = new StringBuilder();

        final boolean isAbsolutePath = path != null && (path.startsWith("http://") || path.startsWith("https://"));
        if (!isAbsolutePath) {
            if (scheme != null) {
                result.append(scheme);

                if (!scheme.endsWith("://")) {
                    result.append("://");
                }
            }

            if (host != null) {
                result.append(host);
            }
        }

        if (port != null) {
            result.append(":");
            result.append(port);
        }

        if (path != null) {
            if (result.length() != 0 && !path.startsWith("/")) {
                result.append('/');
            }
            result.append(path);
        }

        if (query != null) {
            result.append(query);
        }

        return result.toString();
    }

    /**
     * Parse a UrlBuilder from the provided URL string.
     * @param url The string to parse.
     * @return The UrlBuilder that was parsed from the string.
     */
    public static UrlBuilder parse(String url) {
        final UrlBuilder result = new UrlBuilder();

        if (url != null) {
            int startIndex = 0;

            final int protocolEndIndex = url.indexOf("://", startIndex);
            if (protocolEndIndex != -1) {
                result.withScheme(url.substring(startIndex, protocolEndIndex));
                startIndex = protocolEndIndex + 3;
            }

            final int portColonIndex = url.indexOf(':', startIndex);
            if (portColonIndex != -1) {
                result.withHost(url.substring(startIndex, portColonIndex));
                startIndex = portColonIndex + 1;

                final int pathStartSlashIndex = url.indexOf('/', startIndex);
                if (pathStartSlashIndex != -1) {
                    result.withPort(Integer.valueOf(url.substring(portColonIndex + 1, pathStartSlashIndex)));
                    parsePathAndQueryParameters(url, pathStartSlashIndex, result);
                }
                else {
                    final int queryQuestionMarkIndex = url.indexOf('?', startIndex);
                    if (queryQuestionMarkIndex != -1) {
                        result.withPort(Integer.valueOf(url.substring(startIndex, queryQuestionMarkIndex)));
                        parseQueryParameters(url, queryQuestionMarkIndex, result);
                    }
                    else {
                        result.withPort(Integer.valueOf(url.substring(portColonIndex + 1)));
                    }
                }
            }
            else {
                final int pathStartSlashIndex = url.indexOf('/', startIndex);
                if (pathStartSlashIndex != -1) {
                    result.withHost(url.substring(startIndex, pathStartSlashIndex));
                    parsePathAndQueryParameters(url, pathStartSlashIndex, result);
                }
                else {
                    final int queryQuestionMarkIndex = url.indexOf('?', startIndex);
                    if (queryQuestionMarkIndex != -1) {
                        result.withHost(url.substring(startIndex, queryQuestionMarkIndex));
                        parseQueryParameters(url, queryQuestionMarkIndex, result);
                    }
                    else {
                        result.withHost(url.substring(startIndex));
                    }
                }
            }
        }

        return result;
    }

    private static int parsePathAndQueryParameters(String url, int pathSlashIndex, UrlBuilder builder) {
        int startIndex = pathSlashIndex + 1;

        final int queryQuestionMarkIndex = url.indexOf('?', startIndex);
        if (queryQuestionMarkIndex != -1) {
            builder.withPath(url.substring(pathSlashIndex, queryQuestionMarkIndex));
            parseQueryParameters(url, queryQuestionMarkIndex, builder);
        }
        else {
            builder.withPath(url.substring(pathSlashIndex));
        }

        return startIndex;
    }

    private static int parseQueryParameters(String url, int questionMarkIndex, UrlBuilder builder) {
        int startIndex = questionMarkIndex + 1;

        int queryAmpersandIndex = -1;
        do {
            final int queryEqualsSignIndex = url.indexOf('=', startIndex);
            if (queryEqualsSignIndex != -1) {
                final String queryParameterName = url.substring(startIndex, queryEqualsSignIndex);
                startIndex = queryEqualsSignIndex + 1;

                queryAmpersandIndex = url.indexOf('&', startIndex);

                String queryParameterValue;
                if (queryAmpersandIndex != -1) {
                    queryParameterValue = url.substring(startIndex, queryAmpersandIndex);
                    startIndex = queryAmpersandIndex + 1;
                } else {
                    queryParameterValue = url.substring(startIndex);
                    startIndex = -1;
                }
                builder.withQueryParameter(queryParameterName, queryParameterValue);
            }
        } while (queryAmpersandIndex != -1);

        return startIndex;
    }
}
