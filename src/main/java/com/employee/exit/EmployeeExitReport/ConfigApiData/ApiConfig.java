package com.employee.exit.EmployeeExitReport.ConfigApiData;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class ApiConfig {

    // Darwin API
    @Value("${api.darwin.url}")
    private String darwinUrl;

    @Value("${api.darwin.api-key}")
    private String darwinApiKey;

    @Value("${api.darwin.dataset-key}")
    private String darwinDatasetKey;

    @Value("${api.darwin.auth-header}")
    private String darwinAuthHeader;

    // GroXStream API
    @Value("${api.groxstream.url}")
    private String groxStreamUrl;

    @Value("${api.groxstream.api-key}")
    private String groxStreamApiKey;

    @Value("${api.ugro.vendor.url}")
    private String ugroVendorUrl;

    @Value("${api.ugro.vendor.apiKey}")
    private String ugroVendorApiKey;

    @Value("${api.nach.url}")
    private String ugroNachUrl;

    @Value("${api.nach.apiKey}")
    private String ugroNachApiKey;

    public Map<String, String> getUgroVendorHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.ACCEPT, "application/json");
        headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.put("x-api-key", ugroVendorApiKey);
        return headers;
    }

    public Map<String, String> getnachHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.ACCEPT, "application/json");
        headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.put("x-api-key", ugroNachApiKey);
        return headers;
    }

    // Common method to get headers
    public Map<String, String> getDarwinHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", darwinAuthHeader);
        return headers;
    }
    public String getDarwinUrl() {
        return darwinUrl;
    }

    public String getUgroVendorUrl() {
        return ugroVendorUrl;
    }

    public String getDarwinApiKey() {
        return darwinApiKey;
    }

    public String getNachApiUrl() {
        return ugroNachUrl;
    }

    public String getDarwinDatasetKey() {
        return darwinDatasetKey;
    }

    public String getGroxStreamUrl() {
        return groxStreamUrl;
    }
    public Map<String, String> getGroxStreamHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("x-api-key", groxStreamApiKey);
        return headers;
    }
}
