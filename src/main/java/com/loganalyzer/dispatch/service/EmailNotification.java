package com.loganalyzer.dispatch.service;

import com.loganalyzer.analysis.entity.Anomaly;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailNotification implements NotificationService {

    @Override
    public void dispatch(Anomaly anomaly) {
        // In a real application, inject JavaMailSender here
        log.info("📧 MOCK EMAIL DISPATCHED: Alert for Tenant {} -> Rule Matched: {}, Severity: {}", 
            anomaly.getTenantId(), anomaly.getMatchedRuleId(), anomaly.getSeverityLevel());
            
        // Sending email logic...
    }

    @Override
    public String notificationType() {
        return "EMAIL";
    }
}
