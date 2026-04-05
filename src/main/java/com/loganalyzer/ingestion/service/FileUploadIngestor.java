package com.loganalyzer.ingestion.service;

import com.loganalyzer.core.config.RabbitMQConfig;
import com.loganalyzer.core.dto.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
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
    public void ingestLogs(String tenantId, String source, List<MultipartFile> files) {
        log.info("Starting ingestion for tenant: {}, files count: {}", tenantId, files.size());
        
        for (MultipartFile file : files) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        ingestLine(tenantId, source, line);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to process file: {}", file.getOriginalFilename(), e);
            }
        }
    }

    @Override
    public void ingestLine(String tenantId, String source, String logLine) {
        LogEvent event = LogEvent.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .content(logLine)
                .source(source)
                .timestamp(LocalDateTime.now())
                .build();
                
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.INGESTION_ROUTING_KEY, event);
        log.debug("Published log event to RabbitMQ: {}", event.getId());
    }
}
