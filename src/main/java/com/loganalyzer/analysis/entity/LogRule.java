package com.loganalyzer.analysis.entity;

import com.loganalyzer.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "log_rules")
public class LogRule extends BaseEntity {
    private String ruleName;
    private String regexPattern;
    private String severityLevel; // INFO, WARN, ERROR, FATAL
    private String action; // e.g. "EMAIL_ADMIN"
    private String engineType = "REGEX"; // Default to REGEX to support existing records
    private String llmPrompt; // The prompt instruction for the LLM
}
