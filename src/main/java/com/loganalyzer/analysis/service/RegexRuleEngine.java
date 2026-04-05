package com.loganalyzer.analysis.service;

import com.loganalyzer.analysis.entity.LogRule;
import com.loganalyzer.core.dto.LogEvent;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Component
public class RegexRuleEngine implements RuleEngine {

    @Override
    public boolean evaluate(LogEvent event, LogRule rule) {
        if (!"REGEX".equalsIgnoreCase(rule.getEngineType())) {
            return false;
        }
        if (rule.getRegexPattern() == null || rule.getRegexPattern().isEmpty()) {
            return false;
        }
        Pattern pattern = Pattern.compile(rule.getRegexPattern());
        Matcher matcher = pattern.matcher(event.getContent());
        return matcher.find();
    }

    @Override
    public String engineType() {
        return "REGEX";
    }
}
