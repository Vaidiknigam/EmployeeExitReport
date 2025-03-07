package com.employee.exit.EmployeeExitReport.controller;

import com.employee.exit.EmployeeExitReport.Service.EmployeeExitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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


    @PostMapping("/process-exit")
    public ResponseEntity<Map<String, Object>> processEmployeeExit(@RequestBody Map<String, String> employeeDetails) {
        return employeeExitService.processSingleEmployeeExit(employeeDetails);
    }
}
