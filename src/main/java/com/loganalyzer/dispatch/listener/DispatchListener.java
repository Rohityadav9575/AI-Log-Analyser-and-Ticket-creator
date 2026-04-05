package com.loganalyzer.dispatch.listener;

import com.loganalyzer.analysis.entity.Anomaly;
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

    private final List<NotificationService> notificationServices;

    @RabbitListener(queues = RabbitMQConfig.ANOMALY_QUEUE)
    public void consumeAnomaly(Anomaly anomaly) {
        log.info("Received anomaly from RabbitMQ for dispatching: {}", anomaly.getId());
        
        try {
            // Dispatch based on notification type explicitly requested, or broadcast to all relevant
            for (NotificationService service : notificationServices) {
                // If the LogRule has specific actions, you would check them here.
                // Assuming we want to dispatch to both EMAIL and JIRA if configured:
                if (service.notificationType().equals("EMAIL") || service.notificationType().equals("JIRA")) {
                    service.dispatch(anomaly);
                }
            }
            // Update Anomaly status in DB to true (dispatched = true) if needed.
        } catch (Exception e) {
            log.error("Failed to dispatch anomaly: {}", anomaly.getId(), e);
        }
    }
}
