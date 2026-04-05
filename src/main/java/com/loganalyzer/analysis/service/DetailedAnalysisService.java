package com.loganalyzer.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.analysis.dto.AnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetailedAnalysisService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.api-url}")
    private String llmApiUrl;

    @Value("${llm.model}")
    private String llmModel;

    public AnalysisResult analyzeLogContent(String content) {
        log.info("🔍 Requesting detailed analysis for log content...");
        
        try {
            String prompt = "Evaluate the following log segment and provide a detailed analysis in JSON format. " +
                    "The JSON must have these keys: 'summary', 'errors' (a list), 'solution' (a detailed description), and 'severity' (LOW, MEDIUM, HIGH, CRITICAL).\n\n" +
                    "Log Segment:\n" + content + "\n\n" +
                    "Analyze strictly and identify any operational anomalies, exceptions, or errors.";

            Map<String, Object> body = new HashMap<>();
            body.put("model", llmModel);
            body.put("prompt", prompt);
            body.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(llmApiUrl, HttpMethod.POST, request, String.class);
            
            JsonNode root = objectMapper.readTree(response.getBody());
            String rawResponse = root.path("response").asText("");
            
            log.info("🤖 AI Analysis received: {}", rawResponse);
            
            // Try to parse the LLM's JSON response (heuristically)
            return parseLlmJson(rawResponse);
            
        } catch (Exception e) {
            log.error("Failed to perform detailed analysis", e);
            return AnalysisResult.builder()
                    .logSummary("Failed to analyze")
                    .identifiedErrors(List.of(e.getMessage()))
                    .suggestedSolution("Please check system logs for API failure.")
                    .severity("CRITICAL")
                    .build();
        }
    }

    private AnalysisResult parseLlmJson(String rawText) {
        try {
            // Find the first '{' and last '}' to handle potential conversational prefix/suffix
            int start = rawText.indexOf('{');
            int end = rawText.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String jsonPart = rawText.substring(start, end + 1);
                JsonNode node = objectMapper.readTree(jsonPart);
                
                List<String> errors = new ArrayList<>();
                if (node.has("errors")) {
                    node.get("errors").forEach(e -> {
                        if (e.isObject()) {
                            errors.add(formatNodeText(e.path("message")).isEmpty() ? e.toString() : formatNodeText(e.path("message")));
                        } else {
                            errors.add(e.asText());
                        }
                    });
                }

                JsonNode solutionNode = node.path("solution");
                String solutionText = formatNodeText(solutionNode);

                return AnalysisResult.builder()
                        .logSummary(node.path("summary").asText("No summary provided."))
                        .identifiedErrors(errors)
                        .suggestedSolution(solutionText)
                        .severity(node.path("severity").asText("MEDIUM"))
                        .rawLlmResponse(rawText)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Regex-based JSON parsing failed, returning raw response as summary.", e);
        }
        
        return AnalysisResult.builder()
                .logSummary("Structured parsing failed. Use raw details.")
                .identifiedErrors(List.of("Pattern mismatch in model response"))
                .suggestedSolution("Try again or check LLM output format.")
                .rawLlmResponse(rawText)
                .severity("MEDIUM")
                .build();
    }

    private String formatNodeText(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return "No information provided.";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();
            node.forEach(item -> sb.append("- ").append(formatNodeText(item)).append("\n"));
            return sb.toString().trim();
        }
        if (node.isObject()) {
            StringBuilder sb = new StringBuilder();
            node.fields().forEachRemaining(entry -> {
                sb.append(entry.getKey().replace("_", " ").toUpperCase()).append(":\n")
                  .append(formatNodeText(entry.getValue())).append("\n\n");
            });
            return sb.toString().trim();
        }
        return node.toString();
    }
}
