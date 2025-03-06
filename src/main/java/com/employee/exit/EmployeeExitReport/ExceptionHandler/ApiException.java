package com.employee.exit.EmployeeExitReport.ExceptionHandler;

public class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}
