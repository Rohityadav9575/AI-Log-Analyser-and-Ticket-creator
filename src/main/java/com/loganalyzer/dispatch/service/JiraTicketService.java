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
            fields.put("issuetype", issuetype);
            fields.put("summary", "[Auto-Generated] Anomaly Detected: Rule " + anomaly.getMatchedRuleId());
            fields.put("description", "An anomaly was detected with severity: " + anomaly.getSeverityLevel() + 
                                     "\n\nDetails:\nRule: " + anomaly.getMatchedRuleId() + 
                                     "\nSeverity: " + anomaly.getSeverityLevel() + 
                                     "\nSource: " + anomaly.getSource());

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

    public String createManualTicket(Map<String, String> fields) {
        log.info("🚀 Manually creating JIRA Ticket: {}", fields.get("summary"));

        try {
            String url = jiraBaseUrl + "/rest/api/2/issue";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            String auth = jiraUsername + ":" + jiraApiToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            Map<String, Object> body = new HashMap<>();
            Map<String, Object> finalFields = new HashMap<>();

            Map<String, String> project = new HashMap<>();
            project.put("key", jiraProjectKey);

            Map<String, String> issuetype = new HashMap<>();
            issuetype.put("name", "Bug");

            finalFields.put("project", project);
            finalFields.put("summary", fields.get("summary"));
            finalFields.put("description", fields.get("description"));
            finalFields.put("priority", Map.of("name", fields.getOrDefault("priority", "Medium")));
            finalFields.put("issuetype", issuetype);

            body.put("fields", finalFields);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            if (jiraBaseUrl.contains("your-domain")) {
                log.info("🔨 [MOCK] JIRA Manual Ticket Payload: {}", body);
                return "Successfully created [MOCK] JIRA Ticket.";
            } else {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
                log.info("✅ Successfully created JIRA Ticket: {}", response.getBody());
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("❌ Failed to create manual JIRA Ticket", e);
            throw new RuntimeException("Failed to create Jira ticket: " + e.getMessage());
        }
    }

    @Override
    public String notificationType() {
        return "JIRA";
    }
}
