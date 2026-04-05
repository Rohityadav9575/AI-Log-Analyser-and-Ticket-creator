package com.loganalyzer.analysis.listener;

import com.loganalyzer.analysis.service.AnalysisService;
import com.loganalyzer.core.config.RabbitMQConfig;
import com.loganalyzer.core.dto.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogIngestionListener {

    private final AnalysisService analysisService;

    @RabbitListener(queues = RabbitMQConfig.INGESTION_QUEUE)
    public void consumeLogEvent(LogEvent logEvent) {
        log.debug("Received log event from RabbitMQ: {}", logEvent.getId());
        try {
            analysisService.analyzeLogEvent(logEvent);
        } catch (Exception e) {
            log.error("Failed to analyze log event {}", logEvent.getId(), e);
        }
    }
}
