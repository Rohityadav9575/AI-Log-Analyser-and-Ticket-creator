package com.loganalyzer.dispatch.listener;

import com.loganalyzer.analysis.entity.Anomaly;
import com.loganalyzer.analysis.repository.AnomalyRepository;
import com.loganalyzer.core.config.RabbitMQConfig;
import com.loganalyzer.dispatch.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DispatchListener {

    private final AnomalyRepository anomalyRepository;
    private final List<NotificationService> notificationServices;

    @RabbitListener(queues = RabbitMQConfig.ANOMALY_QUEUE)
    public void consumeAnomaly(Anomaly anomaly) {
        log.info("📥 Dispatcher received anomaly from RabbitMQ: [ID: {}, Rule: {}, Severity: {}]",
                anomaly.getId(), anomaly.getMatchedRuleId(), anomaly.getSeverityLevel());

        try {
            log.info("📢 Triggering notification services for anomaly {}...", anomaly.getId());
            for (NotificationService service : notificationServices) {
                if (service.notificationType().equals("EMAIL") || service.notificationType().equals("JIRA")) {
                    log.info("🔔 Invoking {} notification service...", service.notificationType());
                    service.dispatch(anomaly);
                }
            }
            
            // Mark as dispatched in MongoDB
            anomaly.setDispatched(true);
            anomalyRepository.save(anomaly);
            log.info("✅ Anomaly {} status updated to DISPATCHED=true", anomaly.getId());
            
        } catch (Exception e) {
            log.error("❌ Failed to dispatch anomaly: {}", anomaly.getId(), e);
        }
    }
}
