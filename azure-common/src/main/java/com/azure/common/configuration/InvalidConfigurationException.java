package com.azure.common.configuration;

/**
 * Exception thrown when the {@link ClientConfiguration} is not valid for use with a
 * {@link com.azure.common.ServiceClient}.
 */
public class InvalidConfigurationException extends RuntimeException {
    /**
     * Creates an instance with the given message.
     * @param message String describing why the configuration is invalid.
     */
    public InvalidConfigurationException(String message) {
        super(message);
    }
}
