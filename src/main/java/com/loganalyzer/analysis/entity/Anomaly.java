package com.loganalyzer.analysis.entity;

import com.loganalyzer.core.dto.LogEvent;
import com.loganalyzer.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "anomalies")
public class Anomaly extends BaseEntity {
    @org.springframework.data.mongodb.core.index.Indexed
    private String logEventId;
    private String matchedRuleId;
    private String serviceName;
    private String source;
    private String logLevel;
    @org.springframework.data.mongodb.core.index.Indexed
    private String correlationId;
    private String severityLevel;
    private String logContent;
    private String suggestedSolution;
    @Field(targetType = FieldType.ARRAY)
    private List<LogEvent> contextTrace;
    private String status = "PENDING"; // PENDING, RESOLVED
    private boolean dispatched;
}
