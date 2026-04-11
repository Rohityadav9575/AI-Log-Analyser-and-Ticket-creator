package com.loganalyzer.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.analysis.entity.LogRule;
import com.loganalyzer.core.dto.LogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class LlmRuleEngine implements RuleEngine {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${llm.api-url}")
    private String llmApiUrl;

    @Value("${llm.model}")
    private String llmModel;

    public LlmRuleEngine() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000); // 60 seconds
        factory.setReadTimeout(120000);   // 120 seconds to wait for slow huggingface 7B CPU returns
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean evaluate(LogEvent event, LogRule rule) {
        if (!"LLM".equalsIgnoreCase(rule.getEngineType())) {
            return false;
        }

        if (rule.getLlmPrompt() == null || rule.getLlmPrompt().trim().isEmpty()) {
            log.warn("LLM rule {} has empty prompt, ignoring.", rule.getRuleName());
            return false;
        }

        try {
            // Build the specific instruction prompt combining the user's rule prompt and the log payload
            String finalPrompt = "System Instruction: You are an anomaly detection analyzer. " +
                    "Evaluate the following log segment.\n\n" +
                    "Rule Goal: " + rule.getLlmPrompt() + "\n\n" +
                    "Log Segment:\n" + event.getContent() + "\n\n" +
                    "Evaluate the log segment against the rule goal. If the log segment indicates an anomaly defined by the rule, answer ONLY with the word YES. Otherwise, answer ONLY with the word NO. Do not explain your answer.";

            Map<String, Object> body = new HashMap<>();
            body.put("model", llmModel);
            body.put("prompt", finalPrompt);
            body.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("🤔 Sending log {} to LLM ({}) for evaluation...", event.getId(), llmModel);
            log.info("📝 LLM Prompt: {}", finalPrompt);
            log.info("⏳ Waiting for response from model...");
            
            ResponseEntity<String> response = restTemplate.exchange(llmApiUrl, HttpMethod.POST, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String llmResponse = root.path("response").asText("").trim().toUpperCase();

            log.info("🤖 Model Response: {}", llmResponse);

            return llmResponse.contains("YES") && !llmResponse.contains("NO");

        } catch (Exception e) {
            log.error("Failed to evaluate LLM rule {} against log {}", rule.getRuleName(), event.getId(), e);
            return false;
        }
    }

    @Override
    public String engineType() {
        return "LLM";
    }
}
