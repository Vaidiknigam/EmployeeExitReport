package com.employee.exit.EmployeeExitReport.ExceptionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all generic exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", ex.getMessage());
    }

    /**
     * Handles API-related exceptions.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        return buildResponse(HttpStatus.BAD_GATEWAY, "API error", ex.getMessage());
    }

    /**
     * Handles employee not found exceptions.
     */
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEmployeeNotFoundException(EmployeeNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Employee not found", ex.getMessage());
    }

    //email exception
    @ExceptionHandler(EmailException.class)
    public ResponseEntity<Map<String, Object>> handleEmailException(EmailException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Email error", ex.getMessage());
    }


    /**
     * Utility method to build a structured response.
     */
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        return new ResponseEntity<>(response, status);
    }
}
