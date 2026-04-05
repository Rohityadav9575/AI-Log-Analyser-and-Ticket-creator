package com.loganalyzer.analysis.service;

import com.loganalyzer.analysis.entity.LogRule;
import com.loganalyzer.core.dto.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexRuleEngineTest {

    private RegexRuleEngine regexRuleEngine;

    @BeforeEach
    void setUp() {
        regexRuleEngine = new RegexRuleEngine();
    }

    @Test
    void testEvaluate_PatternMatched() {
        // Arrange
        LogEvent event = LogEvent.builder()
                .content("Exception: java.lang.NullPointerException at com.loganalyzer.AuthService")
                .build();
        
        LogRule rule = new LogRule();
        rule.setRegexPattern("NullPointerException");
        
        // Act
        boolean result = regexRuleEngine.evaluate(event, rule);
        
        // Assert
        assertTrue(result, "Engine should return true when pattern matches content");
    }

    @Test
    void testEvaluate_PatternNotMatched() {
        // Arrange
        LogEvent event = LogEvent.builder()
                .content("User login successful for user@example.com")
                .build();
        
        LogRule rule = new LogRule();
        rule.setRegexPattern("Exception|Error");
        
        // Act
        boolean result = regexRuleEngine.evaluate(event, rule);
        
        // Assert
        assertFalse(result, "Engine should return false when pattern does not match content");
    }

    @Test
    void testEvaluate_EmptyPattern() {
        // Arrange
        LogEvent event = LogEvent.builder()
                .content("Normal log line")
                .build();
        
        LogRule rule = new LogRule();
        rule.setRegexPattern("");
        
        // Act
        boolean result = regexRuleEngine.evaluate(event, rule);
        
        // Assert
        assertFalse(result, "Engine should gracefully handle empty regex strings");
    }
}
