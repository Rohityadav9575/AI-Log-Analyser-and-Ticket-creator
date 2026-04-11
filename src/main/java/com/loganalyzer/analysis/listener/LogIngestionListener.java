package com.loganalyzer.analysis.listener;

import com.loganalyzer.analysis.service.AnalysisService;
import com.loganalyzer.core.config.RabbitMQConfig;
import com.loganalyzer.core.dto.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogIngestionListener {

    private final AnalysisService analysisService;
    private final RedisTemplate<String, LogEvent> redisTemplate;

    @RabbitListener(queues = RabbitMQConfig.INGESTION_QUEUE)
    public void consumeLogEvent(LogEvent logEvent) {
        log.info("📥 Received log event from RabbitMQ: [ID: {}, Source: {}, Service: {}, Level: {}]", 
                logEvent.getId(), logEvent.getSource(), logEvent.getServiceName(), logEvent.getLogLevel());
        
        try {
            // ==========================================
            // FAST PATH: Buffer in Redis (Trace Memory)
            // ==========================================
            if (logEvent.getCorrelationId() != null) {
                String traceKey = "logs:trace:" + logEvent.getCorrelationId();
                redisTemplate.opsForList().rightPush(traceKey, logEvent);
                redisTemplate.expire(traceKey, Duration.ofMinutes(60)); // Cache for 1 hour
            }

            // ==========================================
            // FILTER: Only analyze WARN/ERROR or specific rules
            // ==========================================
            boolean shouldAnalyze = "ERROR".equalsIgnoreCase(logEvent.getLogLevel()) || 
                                    "WARN".equalsIgnoreCase(logEvent.getLogLevel()) ||
                                    "FATAL".equalsIgnoreCase(logEvent.getLogLevel());

            if (shouldAnalyze) {
                log.info("⚡ High priority log detected. Triggering Deep Analysis...");
                analysisService.analyze(logEvent);
            } else {
                log.debug("⏭️ INFO log buffered. Skipping deep analysis.");
            }
        } catch (Exception e) {
            log.error("❌ Failed to process log event {}: {}", logEvent.getId(), e.getMessage());
        }
    }
}
