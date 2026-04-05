package com.loganalyzer.ingestion.controller;

import com.loganalyzer.ingestion.service.LogIngestor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final LogIngestor logIngestor;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadLogs(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("source") String source,
            @RequestParam("files") List<MultipartFile> files) {
        
        // In a real scenario, X-Tenant-ID comes from the JWT Token via SecurityContext
        logIngestor.ingestLogs(tenantId, source, files);
        
        return ResponseEntity.ok("Files uploaded and queued for processing successfully.");
    }
}
