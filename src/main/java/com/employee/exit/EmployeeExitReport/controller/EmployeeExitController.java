package com.employee.exit.EmployeeExitReport.controller;

import com.employee.exit.EmployeeExitReport.Service.EmployeeExitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/employee-exit")
public class EmployeeExitController {

    private final EmployeeExitService employeeExitService;
    public EmployeeExitController(EmployeeExitService employeeExitService) {
        this.employeeExitService = employeeExitService;
    }

    @GetMapping("/schedule-report")
    public ResponseEntity<Map<String, String>> scheduleReport() {
        employeeExitService.scheduledEmployeeExitProcess();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Report scheduled!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/generate-report")
    public ResponseEntity<Map<String, String>> generateAndSendReport() {
        employeeExitService.processEmployeeExits();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Report generated and email sent!");
        return ResponseEntity.ok(response);
    }


    @PostMapping("/process-exit")
    public ResponseEntity<Map<String, Object>> processEmployeeExit(@RequestBody Map<String, String> employeeDetails) {
        return employeeExitService.processSingleEmployeeExit(employeeDetails);
    }
}
