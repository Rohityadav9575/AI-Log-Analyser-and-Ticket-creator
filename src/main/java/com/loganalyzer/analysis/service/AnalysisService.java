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
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final LogRuleRepository ruleRepository;
    private final AnomalyRepository anomalyRepository;
    private final List<RuleEngine> ruleEngines; // Inject all implementations of RuleEngine
    private final RabbitTemplate rabbitTemplate;

    public void analyzeLogEvent(LogEvent event) {
        log.debug("Analyzing log event: {}", event.getId());
        List<LogRule> cachedRules = getRulesForTenant(event.getTenantId());

        for (LogRule rule : cachedRules) {
            for (RuleEngine engine : ruleEngines) {
                if (engine.evaluate(event, rule)) {
                    log.warn("Anomaly detected! Rule matched: {}", rule.getRuleName());
                    
                    Anomaly anomaly = new Anomaly();
                    anomaly.setLogEventId(event.getId());
                    anomaly.setMatchedRuleId(rule.getId());
                    anomaly.setSource(event.getSource());
                    anomaly.setTenantId(event.getTenantId());
                    anomaly.setSeverityLevel(rule.getSeverityLevel());
                    anomaly.setLogContent(event.getContent());
                    anomaly.setDispatched(false);
                    
                    Anomaly savedAnomaly = anomalyRepository.save(anomaly);
                    
                    // Publish anomaly for dispatch service
                    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ANOMALY_ROUTING_KEY, savedAnomaly);
                    
                    // Stop checking other rules if we just want one match per log line, 
                    // or remove the break if multiple matches are desired.
                    break;
                }
            }
        }
    }

    @Cacheable(value = "tenantRules", key = "#tenantId")
    public List<LogRule> getRulesForTenant(String tenantId) {
        log.info("Fetching rules from DB for tenant: {}", tenantId);
        return ruleRepository.findByTenantId(tenantId);
    }
}
