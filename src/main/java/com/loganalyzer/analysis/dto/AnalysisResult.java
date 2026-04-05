package com.loganalyzer.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResult {
    private String logSummary;
    private List<String> identifiedErrors;
    private String suggestedSolution;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String rawLlmResponse;
}
