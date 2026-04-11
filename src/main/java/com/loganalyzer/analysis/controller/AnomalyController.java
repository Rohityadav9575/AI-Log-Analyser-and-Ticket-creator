package com.loganalyzer.analysis.controller;

import com.loganalyzer.analysis.entity.Anomaly;
import com.loganalyzer.analysis.repository.AnomalyRepository;
import com.loganalyzer.core.dto.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyRepository anomalyRepository;

    @GetMapping
    public List<Anomaly> getAnomalies(@RequestParam(value = "status", defaultValue = "PENDING") String status) {
        log.info("📊 Fetching anomalies with status: {}", status);
        return anomalyRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Anomaly> updateStatus(
            @PathVariable String id,
            @RequestParam("status") String status) {
        
        return anomalyRepository.findById(id).map(anomaly -> {
            log.info("✅ Marking anomaly {} as {}", id, status);
            anomaly.setStatus(status);
            return ResponseEntity.ok(anomalyRepository.save(anomaly));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/trace/{correlationId}")
    public List<LogEvent> getTrace(@PathVariable String correlationId) {
        log.info("🔍 Fetching trace from DB context for: {}", correlationId);
        return anomalyRepository.findAll().stream()
                .filter(a -> correlationId.equals(a.getCorrelationId()))
                .findFirst()
                .map(Anomaly::getContextTrace)
                .orElse(List.of());
    }
}
