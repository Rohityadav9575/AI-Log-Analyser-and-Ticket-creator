package com.loganalyzer.ingestion.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface LogIngestor {
    void ingestLogs(String tenantId, String source, List<MultipartFile> files);
    void ingestLine(String tenantId, String source, String logLine);
}
