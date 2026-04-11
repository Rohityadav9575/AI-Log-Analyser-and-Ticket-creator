package com.loganalyzer.ingestion.controller;

import com.loganalyzer.ingestion.service.LogIngestor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final LogIngestor logIngestor;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadLogs(
            @RequestParam("source") String source,
            @RequestParam("files") List<MultipartFile> files) {
            
        log.info("📥 Direct ingestion request received from source: {}", source);
        logIngestor.ingestLogs(source, files);
        return ResponseEntity.ok("Ingestion started for " + files.size() + " files");
    }
}
