package io.harness.repositories.entitysetupusage;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface EntitySetupUsageRepository
    extends PagingAndSortingRepository<EntitySetupUsage, String>, EntitySetupUsageCustomRepository {
  long deleteByReferredEntityFQNAndReferredEntityTypeAndReferredByEntityFQNAndReferredByEntityTypeAndAccountIdentifier(
      String referredEntityFQN, String referredEntityType, String referredByEntityFQN, String referredByEntityType,
      String accountIdentifier);

  long deleteByReferredByEntityFQNAndReferredByEntityTypeAndAccountIdentifier(
      String referredByEntityFQN, String referredByEntityType, String accountIdentifier);

  boolean existsByReferredEntityFQNAndReferredEntityTypeAndAccountIdentifier(
      String referredEntityFQN, String referredEntityType, String accountIdentifier);

  long deleteAllByAccountIdentifierAndReferredByEntityFQNAndReferredByEntityTypeAndReferredEntityType(
      String accountIdentifier, String referredByEntityFQN, String referredByEntityType, String referredEntityType);
}
