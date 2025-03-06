package com.employee.exit.EmployeeExitReport.ExceptionHandler;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
