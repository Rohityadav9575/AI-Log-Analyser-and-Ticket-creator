package com.loganalyzer.dispatch.service;

import com.loganalyzer.analysis.entity.Anomaly;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JiraTicketService implements NotificationService {

    private final RestTemplate restTemplate;

    @Value("${jira.base-url}")
    private String jiraBaseUrl;

    @Value("${jira.username}")
    private String jiraUsername;

    @Value("${jira.api-token}")
    private String jiraApiToken;

    @Value("${jira.project-key}")
    private String jiraProjectKey;

    public JiraTicketService() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void dispatch(Anomaly anomaly) {
        log.info("🚀 Dispatching JIRA Ticket for Anomaly: {}", anomaly.getId());

        try {
            String url = jiraBaseUrl + "/rest/api/2/issue";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            // Basic Auth with username:token
            String auth = jiraUsername + ":" + jiraApiToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            // Constructing Jira Payload
            Map<String, Object> body = new HashMap<>();
            Map<String, Object> fields = new HashMap<>();

            Map<String, String> project = new HashMap<>();
            project.put("key", jiraProjectKey);

            Map<String, String> issuetype = new HashMap<>();
            issuetype.put("name", "Bug"); // Changed to Bug as requested

            fields.put("project", project);
            fields.put("summary", "[Auto-Generated] Anomaly Detected: Rule " + anomaly.getMatchedRuleId());
            fields.put("description", "An anomaly was detected with severity: " + anomaly.getSeverityLevel() + 
                                     "\n\nDetails:\nTenant ID: " + anomaly.getTenantId() + 
                                     "\nLog Source: " + anomaly.getSource() +
                                     "\nMessage: " + anomaly.getLogContent());
            fields.put("issuetype", issuetype);

            body.put("fields", fields);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // Using dummy logging if not a real endpoint, else actually make the call
            if (jiraBaseUrl.contains("your-domain")) {
                log.info("🔨 [MOCK] JIRA Ticket Payload: {}", body);
            } else {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
                log.info("✅ Successfully created JIRA Ticket: {}", response.getBody());
            }

        } catch (Exception e) {
            log.error("❌ Failed to create JIRA Ticket for anomaly {}", anomaly.getId(), e);
        }
    }

    @Override
    public String notificationType() {
        return "JIRA";
    }
}
