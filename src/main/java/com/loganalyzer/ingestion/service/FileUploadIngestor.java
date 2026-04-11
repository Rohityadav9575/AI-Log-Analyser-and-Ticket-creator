package com.loganalyzer.ingestion.service;

import com.loganalyzer.core.config.RabbitMQConfig;
import com.loganalyzer.core.dto.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadIngestor implements LogIngestor {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void ingestLogs(String source, List<MultipartFile> files) {
        log.info("Starting ingestion for source: {}, files count: {}", source, files.size());
        
        for (MultipartFile file : files) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        ingestLine(source, line);
                    }
                }
            } catch (IOException e) {
                log.error("Failed to read file {}: {}", file.getOriginalFilename(), e.getMessage());
            }
        }
    }

    @Override
    public void ingestLine(String source, String logLine) {
        try {
            LogEvent event = LogEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .serviceName("detect-from-source")
                    .source(source)
                    .content(logLine)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            log.debug("Publishing log event to RabbitMQ: {}", event.getId());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.INGESTION_ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("Failed to ingest log line: {}", e.getMessage());
        }
    }
}
