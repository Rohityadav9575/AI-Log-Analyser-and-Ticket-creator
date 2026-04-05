package com.loganalyzer.auth.repository;

import com.loganalyzer.auth.entity.Tenant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends MongoRepository<Tenant, String> {
}
