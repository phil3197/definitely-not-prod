package com.definitelynotprod.exception;

public class DefinitionLoadException extends RuntimeException {

    public DefinitionLoadException(String message) {
        super(message);
    }

    public DefinitionLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
