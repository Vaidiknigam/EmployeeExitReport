package com.employee.exit.EmployeeExitReport.controller;

import com.employee.exit.EmployeeExitReport.Service.EmployeeExitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employee-exit")
public class EmployeeExitController {

    private final EmployeeExitService employeeExitService;
    public EmployeeExitController(EmployeeExitService employeeExitService) {
        this.employeeExitService = employeeExitService;
    }
    @GetMapping("/schedule-report")
    public String scheduleReport() {
        employeeExitService.scheduledEmployeeExitProcess();
        return "Report scheduled!";
    }

    @GetMapping("/generate-report")
    public String generateAndSendReport() {
        employeeExitService.processEmployeeExits();
        return "Report generated and email sent!";
    }

//    @GetMapping("/process-exit/{empId}")
//    public String processEmployeeExit(@PathVariable String empId) {
//        return employeeExitService.processSingleEmployeeExit(empId);
//    }
}
