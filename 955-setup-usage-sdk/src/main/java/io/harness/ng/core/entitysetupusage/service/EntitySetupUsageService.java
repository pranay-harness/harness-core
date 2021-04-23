package io.harness.ng.core.entitysetupusage.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entitysetupusage.dto.EntityReferencesDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import java.util.List;
import org.springframework.data.domain.Page;

@OwnedBy(DX)
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

  EntityReferencesDTO listAllReferredUsagesBatch(String accountIdentifier, List<String> referredByEntityFQNList,
      EntityType referredByEntityType, EntityType referredEntityType);
}
