package io.harness.ng.core.entitysetupusage.impl;

import static java.lang.String.format;

import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.entitysetupusage.EntitySetupUsageFilterHelper;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage.EntitySetupUsageKeys;
import io.harness.ng.core.entitysetupusage.mappers.EntitySetupUsageDTOtoEntity;
import io.harness.ng.core.entitysetupusage.mappers.EntitySetupUsageEntityToDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.repositories.entitysetupusage.EntitySetupUsageRepository;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class EntitySetupUsageServiceImpl implements EntitySetupUsageService {
  EntitySetupUsageFilterHelper entitySetupUsageFilterHelper;
  EntitySetupUsageRepository entityReferenceRepository;
  EntitySetupUsageEntityToDTO setupUsageEntityToDTO;
  EntitySetupUsageDTOtoEntity entitySetupUsageDTOtoEntity;

  @Override
  public Page<EntitySetupUsageDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, String searchTerm) {
    String referredEntityFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return listAllEntityUsage(page, size, accountIdentifier, referredEntityFQN, searchTerm);
  }

  @Override
  public Page<EntitySetupUsageDTO> listAllEntityUsage(
      int page, int size, String accountIdentifier, String referredEntityFQN, String searchTerm) {
    Criteria criteria =
        entitySetupUsageFilterHelper.createCriteriaFromEntityFilter(accountIdentifier, referredEntityFQN, searchTerm);
    Pageable pageable = getPageRequest(page, size, Sort.by(Sort.Direction.DESC, EntitySetupUsageKeys.createdAt));
    Page<EntitySetupUsage> entityReferences = entityReferenceRepository.findAll(criteria, pageable);
    return entityReferences.map(entityReference -> setupUsageEntityToDTO.createEntityReferenceDTO(entityReference));
  }

  @Override
  public Boolean isEntityReferenced(String accountIdentifier, String referredEntityFQN) {
    return entityReferenceRepository.existsByReferredEntityFQN(referredEntityFQN);
  }

  @Override
  public EntitySetupUsageDTO save(EntitySetupUsageDTO entitySetupUsageDTO) {
    EntitySetupUsage entitySetupUsage = entitySetupUsageDTOtoEntity.toEntityReference(entitySetupUsageDTO);
    EntitySetupUsage savedEntitySetupUsage = null;
    try {
      savedEntitySetupUsage = entityReferenceRepository.save(entitySetupUsage);
    } catch (DuplicateKeyException ex) {
      log.info(String.format("Error while saving the reference entity [%s]", ex.getMessage()));
      throw new DuplicateFieldException(format("Entity Reference already exists for entity [%s], referredBy [%s]",
          entitySetupUsage.getReferredEntityFQN(), entitySetupUsage.getReferredByEntityFQN()));
    }
    return setupUsageEntityToDTO.createEntityReferenceDTO(savedEntitySetupUsage);
  }

  @Override
  public Boolean delete(String accountIdentifier, String referredEntityFQN, String referredByEntityFQN) {
    long numberOfRecordsDeleted = 0;
    numberOfRecordsDeleted = entityReferenceRepository.deleteByReferredEntityFQNAndReferredByEntityFQN(
        referredEntityFQN, referredByEntityFQN);
    log.info("Deleted {} records for the referred entity {}, referredBy {}", numberOfRecordsDeleted, referredEntityFQN,
        referredByEntityFQN);
    return numberOfRecordsDeleted > 0;
  }

  @Override
  public Boolean deleteAllReferredByEntityRecords(String accountIdentifier, String referredByEntityFQN) {
    long numberOfRecordsDeleted = 0;
    numberOfRecordsDeleted = entityReferenceRepository.deleteByReferredByEntityFQN(referredByEntityFQN);
    return numberOfRecordsDeleted > 0;
  }

  private Pageable getPageRequest(int page, int size, Sort sort) {
    return PageRequest.of(page, size, sort);
  }
}
