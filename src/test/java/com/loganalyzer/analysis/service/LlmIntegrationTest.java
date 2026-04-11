package com.loganalyzer.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.analysis.entity.Anomaly;
import com.loganalyzer.dispatch.service.JiraTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmIntegrationTest {

    @Test
    void testLlmConnection() throws Exception {
        // Load properties dynamically so tokens are never hardcoded here
        Properties props = new Properties();
        try (InputStream is = LlmIntegrationTest.class.getClassLoader().getResourceAsStream("application.properties")) {
            props.load(is);
        }

        String llmUrl = props.getProperty("llm.api-url");
        String model = props.getProperty("llm.model");

        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000);
        factory.setReadTimeout(120000); // Wait up to 120s for CPU inference
        RestTemplate restTemplate = new RestTemplate(factory);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt",
                "Analyze this potential threat in 1 sentence: 'User root logged in from unexpected IP 192.168.x.x'.");
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        System.out.println("🤖 Testing connection to LLM: " + llmUrl);
        ResponseEntity<String> response = restTemplate.exchange(llmUrl, HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful(), "LLM API should return 200 OK");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.getBody());
        String llmResponse = root.path("response").asText();

        System.out.println("✅ LLM Connection Successful! Response was: \n" + llmResponse);
        assertNotNull(llmResponse, "LLM must return a non-null response");
    }

    @Test
    void testLlmAnalysisToJiraTicket() throws Exception {
        Properties props = new Properties();
        try (InputStream is = LlmIntegrationTest.class.getClassLoader().getResourceAsStream("application.properties")) {
            props.load(is);
        }

        // 1. Query LLM for an actual deep analysis of a mock log
        String llmUrl = props.getProperty("llm.api-url");
        String model = props.getProperty("llm.model");

        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000);
        factory.setReadTimeout(120000);
        RestTemplate restTemplate = new RestTemplate(factory);
        ObjectMapper mapper = new ObjectMapper();

        String rawLog = "WARN [Main] DB Connection Pool reached 95% capacity. Slow queries detected on 'Orders' table.";
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt",
                "You are a DevOps AI. Explain the potential impact of this log concisely and suggest 1 resolution: "
                        + rawLog);
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        System.out.println("🤖 Requesting AI Analysis to populate our Jira Description...");
        ResponseEntity<String> response = restTemplate.exchange(llmUrl, HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);
        String aiAnalysis = mapper.readTree(response.getBody()).path("response").asText();
        System.out.println("✅ AI Generated Analysis Successfully!");

        // 2. Execute existing Jira Service natively
        JiraTicketService jiraTicketService = new JiraTicketService();
        ReflectionTestUtils.setField(jiraTicketService, "jiraBaseUrl", props.getProperty("jira.base-url"));
        ReflectionTestUtils.setField(jiraTicketService, "jiraUsername", props.getProperty("jira.username"));
        ReflectionTestUtils.setField(jiraTicketService, "jiraApiToken", props.getProperty("jira.api-token"));
        ReflectionTestUtils.setField(jiraTicketService, "jiraProjectKey", props.getProperty("jira.project-key"));

        System.out.println("🚀 Formatting Anomaly object & dispatching to Jira...");
        Anomaly aiAnomaly = new Anomaly();
        aiAnomaly.setId(UUID.randomUUID().toString().substring(0, 8)); // Short ID
        aiAnomaly.setServiceName("AI_TEST_SERVICE");
        aiAnomaly.setMatchedRuleId("AI_EXPERT_EVALUATION");
        aiAnomaly.setSource("LogAnalyzer Test Suite");
        aiAnomaly.setSeverityLevel("HIGH");

        // **This is where we inject the AI's deep analysis directly into the Jira
        // ticket's Details section!**
        aiAnomaly.setLogContent("RAW LOG EVENT:\n" + rawLog + "\n\n=== AI DEEP ANALYSIS ===\n" + aiAnalysis);

        jiraTicketService.dispatch(aiAnomaly);
        System.out.println("✅ Work complete! Check your Jira instance (Project: "
                + props.getProperty("jira.project-key") + ") for the highly-detailed AI bug.");
    }

    @Test
    void testModelResponse() throws Exception {
        Properties props = new Properties();
        try (InputStream is = LlmIntegrationTest.class.getClassLoader().getResourceAsStream("application.properties")) {
            props.load(is);
        }

        String llmUrl = props.getProperty("llm.api-url");
        String model = props.getProperty("llm.model");

        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt", "Say 'Ollama is alive!' in a creative way.");
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        System.out.println("🤖 Sending creative prompt to: " + llmUrl + " using model: " + model);
        ResponseEntity<String> response = restTemplate.exchange(llmUrl, HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        ObjectMapper mapper = new ObjectMapper();
        String llmResponse = mapper.readTree(response.getBody()).path("response").asText();

        System.out.println("\n✨ Model Response: \n\"" + llmResponse + "\"\n");
        assertNotNull(llmResponse, "Model should return a response");
        assertTrue(!llmResponse.isEmpty(), "Response should not be empty");
    }
}
