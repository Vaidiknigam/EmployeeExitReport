package com.employee.exit.EmployeeExitReport.ExceptionHandler;

public class EmailException extends RuntimeException {
    public EmailException(String message) {
        super(message);
    }
}
