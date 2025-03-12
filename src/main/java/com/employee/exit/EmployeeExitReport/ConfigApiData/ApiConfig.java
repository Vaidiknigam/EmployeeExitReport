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

    @Value("${api.itgov.url}")
    private String ItGovUrl;

    @Value("${api.itgov.apiKey}")
    private String ItGovApiKey;

    @Value("${api.dms.url}")
    private String dmsUrl;

    @Value("${api.dms.apiKey}")
    private String dmsApiKey;

    @Value("${api.groprotect.url}")
    private String groprotectUrl;

    @Value("${api.scf.url}")
    private String scfUrl;

    public String getScfUrl() {
        return scfUrl;
    }

    public void setScfUrl(String scfUrl) {
        this.scfUrl = scfUrl;
    }

    @Value("${api.groprotect.apiKey}")
    private String groprotectApiKey;

    public String getGroprotectUrl() {
        return groprotectUrl;
    }

    public void setGroprotectUrl(String groprotectUrl) {
        this.groprotectUrl = groprotectUrl;
    }

    public String getGroprotectApiKey() {
        return groprotectApiKey;
    }

    public void setGroprotectApiKey(String groprotectApiKey) {
        this.groprotectApiKey = groprotectApiKey;
    }

    public String getDmsUrl() {
        return dmsUrl;
    }

    public void setDmsUrl(String dmsUrl) {
        this.dmsUrl = dmsUrl;
    }

    public String getDmsApiKey() {
        return dmsApiKey;
    }

    public void setDmsApiKey(String dmsApiKey) {
        this.dmsApiKey = dmsApiKey;
    }

    public void setDarwinUrl(String darwinUrl) {
        this.darwinUrl = darwinUrl;
    }

    public void setDarwinApiKey(String darwinApiKey) {
        this.darwinApiKey = darwinApiKey;
    }

    public void setDarwinDatasetKey(String darwinDatasetKey) {
        this.darwinDatasetKey = darwinDatasetKey;
    }

    public String getDarwinAuthHeader() {
        return darwinAuthHeader;
    }

    public void setDarwinAuthHeader(String darwinAuthHeader) {
        this.darwinAuthHeader = darwinAuthHeader;
    }

    public void setGroxStreamUrl(String groxStreamUrl) {
        this.groxStreamUrl = groxStreamUrl;
    }

    public String getGroxStreamApiKey() {
        return groxStreamApiKey;
    }

    public void setGroxStreamApiKey(String groxStreamApiKey) {
        this.groxStreamApiKey = groxStreamApiKey;
    }

    public void setUgroVendorUrl(String ugroVendorUrl) {
        this.ugroVendorUrl = ugroVendorUrl;
    }

    public String getUgroVendorApiKey() {
        return ugroVendorApiKey;
    }

    public void setUgroVendorApiKey(String ugroVendorApiKey) {
        this.ugroVendorApiKey = ugroVendorApiKey;
    }

    public String getUgroNachUrl() {
        return ugroNachUrl;
    }

    public void setUgroNachUrl(String ugroNachUrl) {
        this.ugroNachUrl = ugroNachUrl;
    }

    public String getUgroNachApiKey() {
        return ugroNachApiKey;
    }

    public void setUgroNachApiKey(String ugroNachApiKey) {
        this.ugroNachApiKey = ugroNachApiKey;
    }

    public String getItGovUrl() {
        return ItGovUrl;
    }

    public void setItGovUrl(String itGovUrl) {
        ItGovUrl = itGovUrl;
    }

    public String getItGovApiKey() {
        return ItGovApiKey;
    }

    public void setItGovApiKey(String itGovApiKey) {
        ItGovApiKey = itGovApiKey;
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

    public Map<String, String> getGroxStreamHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("x-api-key", groxStreamApiKey);
        return headers;
    }

    public Map<String, String> getItGovHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("x-api-key", ItGovApiKey);
        return headers;
    }

    public Map<String, String> getDmsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content", "application/json");
        headers.put("Authorization", dmsApiKey);
        return headers;
    }

    public Map<String, String> getGroprotectHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", groprotectApiKey);
        return headers;
    }

    public Map<String, String> getScfHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }
}
