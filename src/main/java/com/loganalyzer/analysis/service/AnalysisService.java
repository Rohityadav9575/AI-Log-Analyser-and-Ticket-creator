package com.loganalyzer.analysis.service;

import com.loganalyzer.analysis.entity.Anomaly;
import com.loganalyzer.analysis.entity.LogRule;
import com.loganalyzer.analysis.repository.AnomalyRepository;
import com.loganalyzer.analysis.repository.LogRuleRepository;
import com.loganalyzer.core.config.RabbitMQConfig;
import com.loganalyzer.core.dto.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final LogRuleRepository ruleRepository;
    private final AnomalyRepository anomalyRepository;
    private final List<RuleEngine> ruleEngines;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, LogEvent> redisTemplate;
    private final DetailedAnalysisService detailedAnalysisService;

    public void analyze(LogEvent event) {
        log.info("🔍 Analyzing log event: [ID: {}, Service: {}, Level: {}]", 
                event.getId(), event.getServiceName(), event.getLogLevel());

        List<LogRule> allRules = ruleRepository.findAll();
        log.info("📋 Found {} total rules in system", allRules.size());

        for (LogRule rule : allRules) {
            log.debug("🕵️ Checking rule '{}' (Type: {}) against log event...", rule.getRuleName(), rule.getEngineType());
            for (RuleEngine engine : ruleEngines) {
                if (engine.evaluate(event, rule)) {
                    log.warn("🚨 Anomaly detected! Rule matched: '{}'", rule.getRuleName());
                    saveAndPublishAnomaly(event, rule.getId(), rule.getSeverityLevel(), null);
                    return; // Stop checking other rules
                }
            }
        }

        // ==========================================
        // SMART FALLBACK: If no rules matched, but it's an ERROR/WARN, analyze anyway!
        // ==========================================
        boolean isHighSeverity = "ERROR".equalsIgnoreCase(event.getLogLevel()) || 
                                 "WARN".equalsIgnoreCase(event.getLogLevel()) ||
                                 "FATAL".equalsIgnoreCase(event.getLogLevel());

        if (isHighSeverity) {
            log.info("📢 No rules matched, but log level is {}. Forcing Deep AI Analysis...", event.getLogLevel());
            saveAndPublishAnomaly(event, "FALLBACK_AI_RULE", event.getLogLevel(), "Log level indicates potential issue.");
        } else {
            log.info("✅ Analysis completed for log {}. No anomalies found.", event.getId());
        }
    }

    private void saveAndPublishAnomaly(LogEvent event, String ruleId, String severity, String reason) {
        Anomaly anomaly = new Anomaly();
        anomaly.setLogEventId(event.getId());
        anomaly.setMatchedRuleId(ruleId);
        anomaly.setServiceName(event.getServiceName());
        anomaly.setSource(event.getSource());
        anomaly.setLogLevel(event.getLogLevel());
        anomaly.setCorrelationId(event.getCorrelationId());
        anomaly.setSeverityLevel(severity);
        anomaly.setLogContent(event.getContent());
        anomaly.setStatus("PENDING");
        
        // Fetch context from Redis
        if (event.getCorrelationId() != null) {
            String traceKey = "logs:trace:" + event.getCorrelationId();
            List<LogEvent> trace = redisTemplate.opsForList().range(traceKey, 0, -1);
            anomaly.setContextTrace(trace);
            
            log.info("🧠 Requesting AI Root Cause Analysis for {} level anomaly...", severity);
            String traceSummary = formatTraceForLlm(trace);
            var result = detailedAnalysisService.analyzeLogContent(traceSummary);
            anomaly.setSuggestedSolution(result.getSuggestedSolution());
        }
        
        anomaly.setDispatched(false);
        Anomaly savedAnomaly = anomalyRepository.save(anomaly);
        log.info("💾 Anomaly saved to DB with ID: {}", savedAnomaly.getId());
        
        // Publish anomaly
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ANOMALY_ROUTING_KEY, savedAnomaly);
    }

    private String formatTraceForLlm(List<LogEvent> trace) {
        StringBuilder sb = new StringBuilder();
        for (LogEvent logItem : trace) {
            sb.append("[").append(logItem.getTimestamp()).append("] ")
              .append(logItem.getServiceName()).append(": ")
              .append(logItem.getContent()).append("\n");
        }
        return sb.toString();
    }

    public List<LogRule> getAllRules() {
        log.info("Fetching all rules from DB");
        return ruleRepository.findAll();
    }
}
