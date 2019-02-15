/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v3.http;

/**
 * A Logger that can be added to an HttpPipeline. This enables each HttpPipelinePolicy to log messages.
 */
public interface HttpPipelineLogger {
    /**
     * @return the log level threshold for what logs will be logged.
     */
    HttpPipelineLogLevel minimumLogLevel();

    /**
     * Log the provided message.
     *
     * @param logLevel The HttpLogDetailLevel associated with this message.
     * @param message The message to log.
     * @param formattedArguments A variadic list of arguments that should be formatted into the
     *                           provided message.
     */
    void log(HttpPipelineLogLevel logLevel, String message, Object... formattedArguments);
}

