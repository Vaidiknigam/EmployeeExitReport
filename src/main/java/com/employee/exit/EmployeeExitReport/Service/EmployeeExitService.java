package com.employee.exit.EmployeeExitReport.Service;

import com.employee.exit.EmployeeExitReport.ConfigApiData.ApiConfig;
import com.employee.exit.EmployeeExitReport.EmailScheduler.EmailSenderService;
import com.employee.exit.EmployeeExitReport.ExceptionHandler.ApiException;
import com.employee.exit.EmployeeExitReport.ExceptionHandler.EmailException;
import com.employee.exit.EmployeeExitReport.RequestHandler.RequestHandler;
import com.employee.exit.EmployeeExitReport.Util.ApiClient;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EmployeeExitService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeExitService.class);
    private final RequestHandler requestHandler;
    private final ApiConfig apiConfig;

    @Autowired
    private EmailSenderService emailSenderService;
    @Autowired
    ApiClient apiClient;


    public EmployeeExitService(EmailSenderService emailSenderService, ApiConfig apiConfig, ApiClient apiClient,RequestHandler requestHandler) {
        this.emailSenderService = emailSenderService;
        this.apiConfig=apiConfig;
        this.apiClient = apiClient;
        this.requestHandler = requestHandler;
    }

    @Scheduled(cron = "0 0 18 * * ?") // Runs daily at 6 PM
    public void scheduledEmployeeExitProcess() {
        try {
            logger.info("Starting scheduled employee exit process...");
            processEmployeeExits();
            logger.info("Scheduled employee exit process completed successfully.");
        } catch (Exception e) {
            logger.error("Scheduled job failed: {}", e.getMessage());
            throw new ApiException("Scheduled job failed: " + e.getMessage());
        }
    }

    public void processEmployeeExits() {
        // Fetch employee data from Darwin API
        List<Map<String, Object>> employeeData = fetchEmployeeData();

        if (employeeData == null || employeeData.isEmpty()) {
            logger.warn("No employee data found. Skipping process.");
            return;
        }

        // Fetch status from GroxStream API
        List<Map<String, Object>> groxStreamStatus = fetchGroxStreamStatus();
        List<Map<String, Object>> ugroVendorStatus = fetchUgroVendorStatus();
        List<Map<String, Object>> ugroNachStatus = fetchNachStatus();

        // Map employeeCode -> status for quick lookup
        Map<String, String> statusMap = new HashMap<>();
        for (Map<String, Object> entry : groxStreamStatus) {
            String employeeCode = entry.get("employeeCode").toString();
            String status = entry.get("status").toString();
            statusMap.put(employeeCode, status);
        }
        Map<String, String> ugroStatusMap = new HashMap<>();
        for (Map<String, Object> entry : ugroVendorStatus) {
            ugroStatusMap.put(entry.get("employeeCode").toString(), entry.get("status").toString());
        }

        Map<String, String> nachStatusMap = new HashMap<>();
        for (Map<String, Object> entry : ugroNachStatus) {
            nachStatusMap.put(entry.get("employeeCode").toString(), entry.get("status").toString());
        }

        // Prepare final report data
        List<Map<String, Object>> reportData = new ArrayList<>();

        for (Map<String, Object> employee : employeeData) {
            String employeeId = employee.get("employee_id") != null ? employee.get("employee_id").toString() : "N/A";
            String employeeName = employee.get("full_name") != null ? employee.get("full_name").toString() : "Unknown";
            String employeeEmailID = employee.get("company_email_id") != null ? employee.get("company_email_id").toString() : "Unknown";
            String employeeCode = employee.get("employee_id") != null ? employee.get("employee_id").toString() : "N/A";

            // Get status from GroxStream response, default to "Pending"
            String groXstreamStatus = statusMap.getOrDefault(employeeCode, "Pending");
            String ugroDeactivateVendorStatus = ugroStatusMap.getOrDefault(employeeCode, "Pending");
            String nachStatus = nachStatusMap.getOrDefault(employeeCode, "Pending");

            Map<String, Object> reportEntry = new HashMap<>();
            reportEntry.put("ID", employeeId);
            reportEntry.put("Name", employeeName);
            reportEntry.put("Email ID", employeeEmailID);
            reportEntry.put("GroXstream Response", groXstreamStatus);
            reportEntry.put("UGro Vendor Response", ugroDeactivateVendorStatus);
            reportEntry.put("Nach Response", nachStatus);

            reportData.add(reportEntry);
        }

        logger.info("Final Report Data: {}", reportData);

        // Generate Excel report with updated data
    String filePath = generateExcelReport(reportData);

        emailSenderService.sendEmailWithAttachment(
                "rishi.khandelwal@ugrocapital.com",
                "Employee Exit Report",
                "Please find the attached Employee Exit Report.",
                filePath
        );

//    String filePath = generateExcelReport(reportData);
//    sendEmailWithAttachment(filePath);
    }


    public List<Map<String, Object>> fetchEmployeeData() {
        String apiUrl = apiConfig.getDarwinUrl();

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("api_key", apiConfig.getDarwinApiKey());
        requestBody.put("datasetKey", apiConfig.getDarwinDatasetKey());
        requestBody.put("last_modified", "01-03-2025 11:00:00");


        try {

            ResponseEntity<Map<String, Object>> responseEntity = apiClient.post(apiUrl, apiConfig.getDarwinHeaders(), requestBody);

            Map<String, Object> response = requestHandler.handleResponse(responseEntity);

            if (response == null || !response.containsKey("employee_data")) {
                throw new ApiException("Invalid API response: Missing 'employee_data' key");
            }

            Object employeeDataObj = response.get("employee_data");
            if (!(employeeDataObj instanceof List)) {
                throw new ApiException("'employee_data' is not a valid list format");
            }

            return (List<Map<String, Object>>) employeeDataObj;

        } catch (Exception e) {
            logger.error("Error fetching employee data: {}", e.getMessage());
            throw new ApiException("Failed to fetch employee data: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> fetchGroxStreamStatus() {
        String apiUrl = apiConfig.getGroxStreamUrl();
        // Fetch employee data from Darwin API
        List<Map<String, Object>> employeeData = fetchEmployeeData();

        if (employeeData == null || employeeData.isEmpty()) {
            logger.warn("No employee data found. Skipping GroxStream API call.");
            return Collections.emptyList();
        }

        // Prepare request body
        List<Map<String, String>> requestBody = new ArrayList<>();
        for (Map<String, Object> employee : employeeData) {
            Map<String, String> requestEntry = new HashMap<>();
            requestEntry.put("employeeEmailId", employee.get("company_email_id").toString());
            requestEntry.put("employeeCode", employee.get("employee_id").toString());
            requestEntry.put("rMEmailId", ""); // Keep rMEmailId as empty

            requestBody.add(requestEntry);
        }

        try {
            // Call GroxStream API
            ResponseEntity<Map<String, Object>> responseEntity = apiClient.post(apiUrl, apiConfig.getGroxStreamHeaders(), requestBody);
            // Handle response
            Map<String, Object> response = requestHandler.handleResponse(responseEntity);
            if (response == null || !response.containsKey("employeeDetails")) {
                throw new ApiException("Invalid API response: Missing 'employeeDetails' key");
            }

            List<Map<String, Object>> employeeDetails = (List<Map<String, Object>>) response.get("employeeDetails");

            // Extract status
            List<Map<String, Object>> statusList = new ArrayList<>();
            for (Map<String, Object> employee : employeeDetails) {
                Map<String, Object> systemResult = (Map<String, Object>) employee.get("systemResult");
                Map<String, Object> groXStream = (Map<String, Object>) systemResult.get("groXStream");

                Map<String, Object> result = new HashMap<>();
                result.put("employeeEmailId", employee.get("employeeEmailId"));
                result.put("employeeCode", employee.get("employeeCode"));
                result.put("status", groXStream.get("status")); // Fetch "status"

                statusList.add(result);
            }

            return statusList;

        } catch (Exception e) {
            logger.error("Error fetching GroxStream status: {}", e.getMessage());
            throw new ApiException("Failed to fetch GroxStream status: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> fetchUgroVendorStatus() {
        String apiUrl = apiConfig.getUgroVendorUrl();

        // Fetch employee data from Darwin API
        List<Map<String, Object>> employeeData = fetchEmployeeData();

        if (employeeData == null || employeeData.isEmpty()) {
            logger.warn("No employee data found. Skipping UGro API call.");
            return Collections.emptyList();
        }

        // Prepare request body
        List<Map<String, String>> requestBody = new ArrayList<>();
        for (Map<String, Object> employee : employeeData) {
            Map<String, String> requestEntry = new HashMap<>();
            requestEntry.put("employeeEmailId", employee.get("company_email_id").toString());
            requestEntry.put("employeeCode", employee.get("employee_id").toString());
            requestEntry.put("rMEmailId", ""); // Keep rMEmailId as empty
            requestBody.add(requestEntry);
        }

        try {
            // Call UGro API
            ResponseEntity<Map<String, Object>> responseEntity = apiClient.post(apiUrl, apiConfig.getUgroVendorHeaders(), requestBody);

            // Handle response
            Map<String, Object> response = requestHandler.handleResponse(responseEntity);
            if (response == null || !response.containsKey("employeeDetails")) {
                throw new ApiException("Invalid API response: Missing 'employeeDetails' key");
            }

            List<Map<String, Object>> employeeDetails = (List<Map<String, Object>>) response.get("employeeDetails");

            // Extract status
            List<Map<String, Object>> statusList = new ArrayList<>();
            for (Map<String, Object> employee : employeeDetails) {
                Map<String, Object> systemResult = (Map<String, Object>) employee.get("systemResult");
                Map<String, Object> partnerPortal = (Map<String, Object>) systemResult.get("partnerPortal");

                Map<String, Object> result = new HashMap<>();
                result.put("employeeEmailId", employee.get("employeeEmailId"));
                result.put("employeeCode", employee.get("employeeCode"));
                result.put("status", partnerPortal.get("status"));

                statusList.add(result);
            }

            return statusList;

        } catch (Exception e) {
            logger.error("Error fetching UGro status: {}", e.getMessage());
            throw new ApiException("Failed to fetch UGro status: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> fetchNachStatus() {
        String apiUrl = apiConfig.getNachApiUrl();

        List<Map<String, Object>> employeeData = fetchEmployeeData(); // Get employee details from DB or another source

        if (employeeData == null || employeeData.isEmpty()) {
            logger.warn("No employee data found. Skipping Nach API call.");
            return Collections.emptyList();
        }

        List<Map<String, String>> requestBody = new ArrayList<>();
        for (Map<String, Object> employee : employeeData) {
            Map<String, String> requestEntry = new HashMap<>();
            requestEntry.put("employeeEmailId", employee.get("company_email_id").toString());
            requestEntry.put("employeeCode", employee.get("employee_id").toString());
            requestEntry.put("rMEmailId", "");
            requestBody.add(requestEntry);
        }

        try {
            ResponseEntity<Map<String, Object>> responseEntity = apiClient.post(apiUrl, apiConfig.getnachHeaders(), requestBody);
            Map<String, Object> response = requestHandler.handleResponse(responseEntity);

            if (response == null || !response.containsKey("employeeDetails")) {
                throw new ApiException("Invalid API response: Missing 'employeeDetails' key");
            }

            List<Map<String, Object>> employeeDetails = (List<Map<String, Object>>) response.get("employeeDetails");
            List<Map<String, Object>> statusList = new ArrayList<>();

            for (Map<String, Object> employee : employeeDetails) {
                Map<String, Object> systemResult = (Map<String, Object>) employee.get("systemResult");
                if (systemResult == null || !systemResult.containsKey("nach")) {
                    logger.warn("Missing 'nach' key for employee: {}", employee.get("employeeCode"));
                    continue;
                }

                Map<String, Object> nach = (Map<String, Object>) systemResult.get("nach");

                Map<String, Object> result = new HashMap<>();
                result.put("employeeEmailId", employee.get("employeeEmailId"));
                result.put("employeeCode", employee.get("employeeCode"));
                result.put("status", nach.get("status"));

                statusList.add(result);
            }

            return statusList;

        } catch (Exception e) {
            logger.error("Error fetching Nach status: {}", e.getMessage());
            throw new ApiException("Failed to fetch Nach status: " + e.getMessage());
        }
    }


    private String generateExcelReport(List<Map<String, Object>> data) {
        String filePath = "EmployeeExitReport.xlsx";
        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(filePath)) {
            Sheet sheet = workbook.createSheet("Employee Exit Report");

            // Creating Header Row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Name");
            headerRow.createCell(2).setCellValue("GroXstream Portal");
            headerRow.createCell(3).setCellValue("Vendor/Partner Portal");
            headerRow.createCell(4).setCellValue("Nach/Payment Portal ");

            // Populating Data Rows
            int rowNum = 1;
            for (Map<String, Object> entry : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.get("ID") != null ? entry.get("ID").toString() : "NULL");
                row.createCell(1).setCellValue(entry.get("Name") != null ? entry.get("Name").toString() : "NULL");
                row.createCell(2).setCellValue(entry.get("GroXstream Response") != null ? entry.get("GroXstream Response").toString() : "NULL");
                row.createCell(3).setCellValue(entry.get("UGro Vendor Response") != null ? entry.get("UGro Vendor Response").toString() : "NULL");
                row.createCell(4).setCellValue(entry.get("Nach Response") != null ? entry.get("Nach Response").toString() : "NULL");
            }

            // Write to File
            workbook.write(fileOut);
            logger.info("Excel report generated successfully at: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to generate Excel report: {}", e.getMessage());
            throw new ApiException("Failed to generate Excel report: " + e.getMessage());
        }
        return filePath;
    }

}
