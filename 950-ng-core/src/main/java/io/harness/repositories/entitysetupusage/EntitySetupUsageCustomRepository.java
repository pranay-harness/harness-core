package io.harness.repositories.entitysetupusage;

import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface EntitySetupUsageCustomRepository {
  Page<EntitySetupUsage> findAll(Criteria criteria, Pageable pageable);
}
