package com.definitelynotprod.controller;

import com.definitelynotprod.controller.dto.ErrorResponse;
import com.definitelynotprod.exception.DefinitionLoadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DefinitionLoadException.class)
    public ResponseEntity<ErrorResponse> handleDefinitionLoadException(DefinitionLoadException exception) {
        log.error("Definition loading failed", exception);
        return ResponseEntity.internalServerError().body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.internalServerError().body(new ErrorResponse("Internal server error"));
    }
}
