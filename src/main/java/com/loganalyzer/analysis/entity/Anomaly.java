package com.loganalyzer.analysis.entity;

import com.loganalyzer.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "anomalies")
public class Anomaly extends BaseEntity {
    private String logEventId;
    private String matchedRuleId;
    private String source;
    private String logContent;
    private String severityLevel;
    private boolean dispatched;
}
