package io.harness.ng.core.entitysetupusage.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface EntitySetupUsageRepository
    extends PagingAndSortingRepository<EntitySetupUsage, String>, EntitySetupUsageCustomRepository {
  long deleteByReferredEntityFQNAndReferredByEntityFQN(String referredEntityFQN, String referredByEntityFQN);
  long deleteByReferredByEntityFQN(String referredByEntityFQN);
  boolean existsByReferredEntityFQN(String referredEntityFQN);
}
