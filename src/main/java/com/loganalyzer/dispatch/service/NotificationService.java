package com.loganalyzer.dispatch.service;

import com.loganalyzer.analysis.entity.Anomaly;

public interface NotificationService {
    void dispatch(Anomaly anomaly);
    String notificationType();
}
