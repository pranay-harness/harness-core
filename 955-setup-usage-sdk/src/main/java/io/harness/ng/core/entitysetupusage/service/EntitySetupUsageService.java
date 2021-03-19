package io.harness.ng.core.entitysetupusage.service;

import io.harness.EntityType;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import java.util.List;
import org.springframework.data.domain.Page;

public interface EntitySetupUsageService {
  Page<EntitySetupUsageDTO> listAllEntityUsage(int page, int size, String accountIdentifier, String referredEntityFQN,
      EntityType referredEntityType, String searchTerm);

  List<EntitySetupUsageDTO> listAllReferredUsages(int page, int size, String accountIdentifier,
      String referredByEntityFQN, EntityType referredEntityType, String searchTerm);

  Page<EntitySetupUsageDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, EntityType referredEntityType, String searchTerm);

  @Deprecated EntitySetupUsageDTO save(EntitySetupUsageDTO entitySetupUsageDTO);

  Boolean delete(String accountIdentifier, String referredEntityFQN, EntityType referredEntityType,
      String referredByEntityFQN, EntityType referredByEntityType);

  Boolean deleteAllReferredByEntityRecords(
      String accountIdentifier, String referredByEntityFQN, EntityType referredByEntityType);

  Boolean isEntityReferenced(String accountIdentifier, String referredEntityFQN, EntityType referredEntityType);

  // todo(abhinav): make delete and create a transactional operation
  Boolean flushSave(List<EntitySetupUsage> entitySetupUsage, EntityType entityTypeFromChannel,
      boolean deleteOldReferredByRecords, String accountId);
}
