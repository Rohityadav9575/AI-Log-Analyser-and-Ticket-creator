package com.loganalyzer.analysis.repository;

import com.loganalyzer.analysis.entity.Anomaly;
import com.loganalyzer.analysis.entity.LogRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRuleRepository extends MongoRepository<LogRule, String> {
}
