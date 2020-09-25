package io.harness.ng.core.entityReference.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.entityReference.entity.EntityReference;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface EntityReferenceRepository
    extends PagingAndSortingRepository<EntityReference, String>, EntityReferenceCustomRepository {
  long deleteByReferredEntityFQNAndReferredByEntityFQN(String referredEntityFQN, String referredByEntityFQN);
  boolean existsByReferredEntityFQN(String referredEntityFQN);
}
