package com.loganalyzer.analysis.repository;

import com.loganalyzer.analysis.entity.Anomaly;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnomalyRepository extends MongoRepository<Anomaly, String> {
}
