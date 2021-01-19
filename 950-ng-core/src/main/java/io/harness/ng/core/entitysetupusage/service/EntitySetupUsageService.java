package io.harness.ng.core.entitysetupusage.service;

import io.harness.EntityType;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;

import org.springframework.data.domain.Page;

public interface EntitySetupUsageService {
  Page<EntitySetupUsageDTO> listAllEntityUsage(int page, int size, String accountIdentifier, String referredEntityFQN,
      EntityType referredEntityType, String searchTerm);

  Page<EntitySetupUsageDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, EntityType referrredEntityType, String searchTerm);

  EntitySetupUsageDTO save(EntitySetupUsageDTO entitySetupUsageDTO);

  Boolean delete(String accountIdentifier, String referredEntityFQN, EntityType referredEntityType,
      String referredByEntityFQN, EntityType referredByEntityType);

  Boolean deleteAllReferredByEntityRecords(
      String accountIdentifier, String referredByEntityFQN, EntityType referredByEntityType);

  Boolean isEntityReferenced(String accountIdentifier, String referredEntityFQN, EntityType referredEntityType);
}
