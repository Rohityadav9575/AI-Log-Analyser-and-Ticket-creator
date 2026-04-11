package com.loganalyzer.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent implements Serializable {
    private String id;
    private String serviceName; // The name of the microservice
    private String source; // e.g. "nginx", "webapp-backend", "drive"
    private String content;
    private String logLevel; // INFO, WARN, ERROR, FATAL
    private String correlationId; // Flow ID for tracing across services
    private LocalDateTime timestamp;
}
