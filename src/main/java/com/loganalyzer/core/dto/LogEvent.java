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
    private String tenantId;
    private String source; // e.g. "nginx", "webapp-backend", "drive"
    private String content;
    private LocalDateTime timestamp;
}
