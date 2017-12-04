package com.microsoft.rest.v2.http;

/**
 * A HttpPipeline RequestPolicy logger that logs to the StdOut/System.out stream.
 */
public class StandardOutLogger extends HttpPipeline.AbstractLogger {
    @Override
    public void log(HttpPipeline.LogLevel logLevel, String message, Object... formattedArguments) {
        System.out.println(logLevel + ") " + format(message, formattedArguments));
    }
}
