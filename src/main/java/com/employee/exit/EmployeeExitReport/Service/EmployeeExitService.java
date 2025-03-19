package com.employee.exit.EmployeeExitReport.Service;

import com.employee.exit.EmployeeExitReport.ConfigApiData.ApiConfig;
import com.employee.exit.EmployeeExitReport.EmailScheduler.EmailSenderService;
import com.employee.exit.EmployeeExitReport.ExceptionHandler.ApiException;
import com.employee.exit.EmployeeExitReport.RequestHandler.RequestResponseHandler;
import com.employee.exit.EmployeeExitReport.Util.ApiClient;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeExitService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeExitService.class);
    private final EmailSenderService emailSenderService;
    private final ApiConfig apiConfig;
    private final ApiClient apiClient;
    private final RequestResponseHandler requestHandler;

    public EmployeeExitService(EmailSenderService emailSenderService, ApiConfig apiConfig, ApiClient apiClient, RequestResponseHandler requestHandler) {
        this.emailSenderService = emailSenderService;
        this.apiConfig = apiConfig;
        this.apiClient = apiClient;
        this.requestHandler = requestHandler;
    }

    @Scheduled(cron = "0 30 19 * * ?") // Runs daily at 7:30 PM
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
        List<Map<String, Object>> employeeData = fetchEmployeeData();
        if (employeeData.isEmpty()) {
            logger.warn("No employee data found. Skipping process.");
            return;
        }
        // Fetch status from APIs
        Map<String, String> groxStreamStatus = fetchStatus(fetchGroxStreamStatus());
        Map<String, String> ugroVendorStatus = fetchStatus(fetchUgroVendorStatus());
        Map<String, String> ugroNachStatus = fetchStatus(fetchNachStatus());
        Map<String, String> ItGovStatus = fetchStatus(fetchItGovStatus());
        Map<String, String> DmsStatus = fetchStatus(fetchDmsStatus());
        Map<String, String> GroProtectStatus = fetchStatus(fetchGroProtectStatus());
        Map<String, String> ScfStatus = fetchStatus(fetchScfStatus());


        List<Map<String, Object>> reportData = employeeData.stream().map(employee -> Map.of(
                "ID", employee.getOrDefault("employee_id", "N/A"),
                "Name", employee.getOrDefault("full_name", "Unknown"),
                "Email ID", employee.getOrDefault("company_email_id", "Unknown"),
                "GroXstream Response", getStatusOrDefault(groxStreamStatus, employee.get("employee_id")),
                "UGro Vendor Response", getStatusOrDefault(ugroVendorStatus, employee.get("employee_id")),
                "Nach Response", getStatusOrDefault(ugroNachStatus, employee.get("employee_id")),
                "IT GOV Response", getStatusOrDefault(ItGovStatus, employee.get("employee_id")),
                "DMS Response", getStatusOrDefault(DmsStatus, employee.get("employee_id")),
                "GroProtect Response", getStatusOrDefault(GroProtectStatus, employee.get("employee_id")),
                "Scf Response", getStatusOrDefault(ScfStatus, employee.get("employee_id"))

        )).collect(Collectors.toList());

        String filePath = generateExcelReport(reportData);
        List<String> recipients = Arrays.asList(
                "vaidik.nigam@ugrocapital.com"
        );
        emailSenderService.sendEmailWithAttachment(
                recipients,
                "Employee Exit Report",
                "Please find the attached Employee Exit Report.", filePath);
    }

    private String getStatusOrDefault(Map<String, String> statusMap, Object employeeId) {
        return statusMap.getOrDefault(employeeId, "Pending");
    }

    private Map<String, String> fetchStatus(List<Map<String, Object>> responseList) {
        return responseList.stream()
                .collect(Collectors.toMap(
                        entry -> entry.get("employeeCode").toString(),
                        entry -> mapStatus(entry.get("status").toString()),
                        (existing, replacement) -> existing  // Handle duplicate keys
                ));
    }

    private String mapStatus(String status) {
        switch (status) {
            case "NA":
                return "Not Available";
            case "deactivated":
                return "Employee is deactivated";
            case "error":
                return "Status is showing error";
            default:
                return "Unknown";
        }
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
    public List<Map<String, Object>> fetchItGovStatus() {
        String apiUrl = apiConfig.getItGovUrl();

        List<Map<String, Object>> employeeData = fetchEmployeeData(); // Fetch employee details

        if (employeeData == null || employeeData.isEmpty()) {
            logger.warn("No employee data found. Skipping IT GOV API call.");
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
            ResponseEntity<Map<String, Object>> responseEntity = apiClient.post(apiUrl, apiConfig.getItGovHeaders(), requestBody);
            Map<String, Object> response = requestHandler.handleResponse(responseEntity);

            if (response == null || !response.containsKey("employeeDetails")) {
                throw new ApiException("Invalid API response: Missing 'employeeDetails' key");
            }

            List<Map<String, Object>> employeeDetails = (List<Map<String, Object>>) response.get("employeeDetails");
            List<Map<String, Object>> statusList = new ArrayList<>();

            for (Map<String, Object> employee : employeeDetails) {
                Map<String, Object> systemResult = (Map<String, Object>) employee.get("systemResult");
                if (systemResult == null || !systemResult.containsKey("IT_GOV")) {
                    logger.warn("Missing 'IT_GOV' key for employee: {}", employee.get("employeeCode"));
                    continue;
                }

                Map<String, Object> itGov = (Map<String, Object>) systemResult.get("IT_GOV");

                Map<String, Object> result = new HashMap<>();
                result.put("employeeEmailId", employee.get("employeeEmailId"));
                result.put("employeeCode", employee.get("employeeCode"));
                result.put("status", itGov.get("status"));

                statusList.add(result);
            }

            return statusList;

        } catch (Exception e) {
            logger.error("Error fetching IT GOV status: {}", e.getMessage());
            throw new ApiException("Failed to fetch IT GOV status: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> fetchDmsStatus() {
            String apiUrl = apiConfig.getDmsUrl();

            List<Map<String, Object>> employeeData = fetchEmployeeData(); // Fetch employee details

            if (employeeData == null || employeeData.isEmpty()) {
                logger.warn("No employee data found. Skipping DMS API call.");
                return Collections.emptyList();
            }

            List<Map<String, Object>> requestBody = new ArrayList<>();
            for (Map<String, Object> employee : employeeData) {
                Map<String, Object> requestEntry = new HashMap<>();
                requestEntry.put("employeeEmailId", employee.get("company_email_id").toString());
                requestEntry.put("employeeCode", employee.get("employee_id").toString());
                requestEntry.put("rMEmailId", "");
                requestBody.add(requestEntry);
            }

            try {
                ResponseEntity<Map<String, Object>> responseEntity = apiClient.post(apiUrl, apiConfig.getDmsHeaders(), requestBody);
                Map<String, Object> response = requestHandler.handleResponse(responseEntity);

                if (response == null || !response.containsKey("employeeDetails")) {
                    throw new ApiException("Invalid API response: Missing 'employeeDetails' key");
                }

                List<Map<String, Object>> employeeDetails = (List<Map<String, Object>>) response.get("employeeDetails");
                List<Map<String, Object>> statusList = new ArrayList<>();

                for (Map<String, Object> employee : employeeDetails) {
                    Map<String, Object> systemResult = (Map<String, Object>) employee.get("systemResult");
                    if (systemResult == null || !systemResult.containsKey("UGRO_DMS")) {  // Checking for "DMS" key
                        logger.warn("Missing 'UGRO_DMS' key for employee: {}", employee.get("employeeCode"));
                        continue;
                    }

                    Map<String, Object> dms = (Map<String, Object>) systemResult.get("UGRO_DMS");

                    Map<String, Object> result = new HashMap<>();
                    result.put("employeeEmailId", employee.get("employeeEmailId"));
                    result.put("employeeCode", employee.get("employeeCode"));
                    result.put("status", dms.get("status")); // Extracting status from "DMS"

                    statusList.add(result);
                }

                return statusList;

            } catch (Exception e) {
                logger.error("Error fetching DMS status: {}", e.getMessage());
                throw new ApiException("Failed to fetch DMS status: " + e.getMessage());
            }
        }

    public List<Map<String, Object>> fetchGroProtectStatus() {
        String apiUrl = apiConfig.getGroprotectUrl();

        List<Map<String, Object>> employeeData = fetchEmployeeData(); // Fetch employee details

        if (employeeData == null || employeeData.isEmpty()) {
            logger.warn("No employee data found. Skipping GRO PROTECT API call.");
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
            ResponseEntity<Map<String, Object>> responseEntity = apiClient.post(apiUrl, apiConfig.getGroprotectHeaders(), requestBody);
            Map<String, Object> response = requestHandler.handleResponse(responseEntity);

            if (response == null || !response.containsKey("employeeDetails")) {
                throw new ApiException("Invalid API response: Missing 'employeeDetails' key");
            }

            List<Map<String, Object>> employeeDetails = (List<Map<String, Object>>) response.get("employeeDetails");
            List<Map<String, Object>> statusList = new ArrayList<>();

            for (Map<String, Object> employee : employeeDetails) {
                Map<String, Object> systemResult = (Map<String, Object>) employee.get("systemResult");
                if (systemResult == null || !systemResult.containsKey("GRO_PROTECT")) {  // Checking for "GRO_PROTECT" key
                    logger.warn("Missing 'GRO_PROTECT' key for employee: {}", employee.get("employeeCode"));
                    continue;
                }

                Map<String, Object> groProtect = (Map<String, Object>) systemResult.get("GRO_PROTECT");

                Map<String, Object> result = new HashMap<>();
                result.put("employeeEmailId", employee.get("employeeEmailId"));
                result.put("employeeCode", employee.get("employeeCode"));
                result.put("status", groProtect.get("status")); // Extracting status from "GRO_PROTECT"

                statusList.add(result);
            }

            return statusList;

        } catch (Exception e) {
            logger.error("Error fetching GRO PROTECT status: {}", e.getMessage());
            throw new ApiException("Failed to fetch GRO PROTECT status: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> fetchScfStatus() {
        String apiUrl = apiConfig.getScfUrl();

        List<Map<String, Object>> employeeData = fetchEmployeeData(); // Fetch employee details

        if (employeeData == null || employeeData.isEmpty()) {
            logger.warn("No employee data found. Skipping SCF API call.");
            return Collections.emptyList();
        }

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, String>> employeeRequests = new ArrayList<>();

        for (Map<String, Object> employee : employeeData) {
            Map<String, String> requestEntry = new HashMap<>();
            requestEntry.put("employeeEmailId", employee.get("company_email_id").toString());
            requestEntry.put("employeeCode", employee.get("employee_id").toString());
            requestEntry.put("rMEmailId", ""); // Placeholder, modify if needed
            employeeRequests.add(requestEntry);
        }

        requestBody.put("employeeRequests", employeeRequests); // Wrapping list inside "employeeRequests" key

        try {
            ResponseEntity<Map<String, Object>> responseEntity = apiClient.post(apiUrl, apiConfig.getScfHeaders(), requestBody);
            Map<String, Object> response = requestHandler.handleResponse(responseEntity);

            if (response == null || !response.containsKey("employeeDetails")) {
                throw new ApiException("Invalid API response: Missing 'employeeDetails' key");
            }

            List<Map<String, Object>> employeeDetails = (List<Map<String, Object>>) response.get("employeeDetails");
            List<Map<String, Object>> statusList = new ArrayList<>();

            for (Map<String, Object> employee : employeeDetails) {
                Map<String, Object> systemResult = (Map<String, Object>) employee.get("systemResult");
                if (systemResult == null || !systemResult.containsKey("scf")) {  // Checking for "SCF" key
                    logger.warn("Missing 'scf' key for employee: {}", employee.get("employeeCode"));
                    continue;
                }

                Map<String, Object> scf = (Map<String, Object>) systemResult.get("scf");

                Map<String, Object> result = new HashMap<>();
                result.put("employeeEmailId", employee.get("employeeEmailId"));
                result.put("employeeCode", employee.get("employeeCode"));
                result.put("status", scf.get("status")); // Extracting status from "SCF"

                statusList.add(result);
            }

            return statusList;

        } catch (Exception e) {
            logger.error("Error fetching SCF status: {}", e.getMessage());
            throw new ApiException("Failed to fetch SCF status: " + e.getMessage());
        }
    }

    public ResponseEntity<Map<String, Object>> processSingleEmployeeExit(@RequestBody Map<String, String> employeeDetails) {
        String employeeCode = employeeDetails.get("employeeCode");
        String employeeEmailId = employeeDetails.get("employeeEmailId");

        // Fetch responses from all three APIs
        List<Map<String, Object>> groxStreamResponse = fetchGroxStreamStatus();
        List<Map<String, Object>> ugroVendorResponse = fetchUgroVendorStatus();
        List<Map<String, Object>> nachResponse = fetchNachStatus();
        List<Map<String, Object>> itGovResponse = fetchItGovStatus();
        List<Map<String, Object>> dmsResponse = fetchDmsStatus();

        // Extract actual status for the employee
        Map<String, Object> groxStreamResult = extractEmployeeStatus(groxStreamResponse, employeeCode);
        Map<String, Object> ugroVendorResult = extractEmployeeStatus(ugroVendorResponse, employeeCode);
        Map<String, Object> nachResult = extractEmployeeStatus(nachResponse, employeeCode);
        Map<String, Object> itGovResult = extractEmployeeStatus(itGovResponse, employeeCode);
        Map<String, Object> dmsResult = extractEmployeeStatus(dmsResponse, employeeCode);

        // Construct the final response
        Map<String, Object> response = new HashMap<>();
        response.put("employeeDetails", List.of(Map.of(
                "employeeEmailId", employeeEmailId,
                "employeeCode", employeeCode,
                "systemResult", Map.of(
                        "groXStream", groxStreamResult,
                        "ugroVendor", ugroVendorResult,
                        "nach", nachResult,
                        "IT_GOV", itGovResult,
                        "UGRO_DMS", dmsResult
                )
        )));

        logger.info("Employee Exit Status Response: {}", response);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> extractEmployeeStatus(List<Map<String, Object>> apiResponse, String employeeCode) {
        return apiResponse.stream()
                .filter(entry -> employeeCode.equals(entry.get("employeeCode")))
                .findFirst()
                .map(entry -> {
                    String status = entry.get("status") != null ? entry.get("status").toString() : "NA";
                    return Map.of(
                            "code", entry.getOrDefault("code", "404"),
                            "status", mapStatus(status)  // Ensure mapping function translates correctly
                    );
                })
                .orElse(Map.of("code", "404", "status", "NA")); // Default if employeeCode not found
    }

    private String generateExcelReport(List<Map<String, Object>> data) {
        String filePath = "EmployeeExitReport.xlsx";
        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(filePath)) {
            Sheet sheet = workbook.createSheet("Employee Exit Report");
            Row headerRow = sheet.createRow(0);
            List<String> headers = List.of("ID/Name", "GroXstream Portal", "Vendor/Partner Portal", "Nach/Payment Portal", "IT GOV Portal", "DMS Portal","GroProtect Portal","SCF Portal");
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }
            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);
            int rowNum = 1;
            for (Map<String, Object> entry : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.get("ID") + " / " + entry.get("Name"));
                row.createCell(1).setCellValue(entry.get("GroXstream Response").toString());
                row.createCell(2).setCellValue(entry.get("UGro Vendor Response").toString());
                row.createCell(3).setCellValue(entry.get("Nach Response").toString());
                row.createCell(4).setCellValue(entry.get("IT GOV Response").toString());
                row.createCell(5).setCellValue(entry.get("DMS Response").toString());
                row.createCell(6).setCellValue(entry.get("GroProtect Response").toString());
                row.createCell(7).setCellValue(entry.get("Scf Response").toString());

                for (int i = 0; i < 8; i++) {
                    row.getCell(i).setCellStyle(borderStyle);
                }
            }
            workbook.write(fileOut);
        } catch (IOException e) {
            throw new ApiException("Failed to generate Excel report: " + e.getMessage());
        }
        return filePath;
    }
}
