package com.loganalyzer.analysis.service;

import com.loganalyzer.analysis.entity.LogRule;
import com.loganalyzer.core.dto.LogEvent;

public interface RuleEngine {
    boolean evaluate(LogEvent event, LogRule rule);
    String engineType();
}
