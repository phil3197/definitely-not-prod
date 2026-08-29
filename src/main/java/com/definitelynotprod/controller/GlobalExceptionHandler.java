package com.definitelynotprod.controller;

import com.definitelynotprod.exception.DefinitionLoadException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DefinitionLoadException.class)
    public ResponseEntity<Map<String, String>> handleDefinitionLoadException(DefinitionLoadException exception) {
        return ResponseEntity.internalServerError().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception exception) {
        return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
    }
}
