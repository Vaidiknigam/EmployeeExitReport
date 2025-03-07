package com.employee.exit.EmployeeExitReport.RequestHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class RequestResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(RequestResponseHandler.class);

    /**
     * Handles API responses based on HTTP status codes.
     *
     * @param responseEntity The response entity from the API call.
     * @return The response body if status is 200 (OK), null for 3xx/4xx, and throws an exception for 5xx.
     */
    public <T> T handleResponse(ResponseEntity<T> responseEntity) {
        HttpStatus status = (HttpStatus) responseEntity.getStatusCode();

        if (status.is2xxSuccessful()) { // 200 - OK
            return responseEntity.getBody();
        } else if (status.is3xxRedirection()) {
            logger.warn("Redirection response received: {}", status);
            return null;
        } else if (status.is4xxClientError()) {
            logger.warn("Client error response received: {}", status);
            return null;
        } else if (status.is5xxServerError()) {
            logger.error("Server error response received: {}", status);
            throw new RuntimeException("Server error occurred: " + status);
        }

        logger.warn("Unexpected response status: {}", status);
        return null;
    }
}
