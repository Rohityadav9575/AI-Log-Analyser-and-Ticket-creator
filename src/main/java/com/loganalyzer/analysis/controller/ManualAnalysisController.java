package com.loganalyzer.analysis.controller;

import com.loganalyzer.analysis.dto.AnalysisResult;
import com.loganalyzer.analysis.service.DetailedAnalysisService;
import com.loganalyzer.dispatch.service.JiraTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/manual")
@RequiredArgsConstructor
public class ManualAnalysisController {

    private final DetailedAnalysisService detailedAnalysisService;
    private final JiraTicketService jiraTicketService;

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResult> analyzeManualLog(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("files") List<MultipartFile> files) {
        log.info("📥 Manual upload for log analysis: {} files", files.size());
        
        try {
            StringBuilder combinedContent = new StringBuilder();
            for (MultipartFile file : files) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                    String content = reader.lines().collect(Collectors.joining("\n"));
                    combinedContent.append("--- File: ").append(file.getOriginalFilename()).append(" ---\n")
                                   .append(content).append("\n\n");
                }
            }
            
            AnalysisResult result = detailedAnalysisService.analyzeLogContent(combinedContent.toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to read log files", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/jira/create")
    public ResponseEntity<String> createManualJiraTicket(@RequestBody Map<String, String> fields) {
        log.info("🎫 Request to create manual JIRA ticket: {}", fields.get("summary"));
        
        try {
            String response = jiraTicketService.createManualTicket(fields);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create manual Jira ticket", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
