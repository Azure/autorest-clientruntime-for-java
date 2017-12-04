package com.microsoft.rest.v2.http;

/**
 * An adapter that connects an HttpPipeline.Logger to an slf4j.Logger.
 */
public class Slf4jLogger extends HttpPipeline.AbstractLogger {
    private final org.slf4j.Logger slf4jLogger;

    public Slf4jLogger(org.slf4j.Logger slf4jLogger) {
        this.slf4jLogger = slf4jLogger;
    }

    @Override
    public void log(HttpPipeline.LogLevel logLevel, String message, Object... formattedArguments) {
        message = format(message, formattedArguments);
        switch (logLevel) {
            case INFO:
                slf4jLogger.info(message);
                break;

            case WARNING:
                slf4jLogger.warn(message);
                break;

            case ERROR:
                slf4jLogger.error(message);
                break;
        }
    }
}
