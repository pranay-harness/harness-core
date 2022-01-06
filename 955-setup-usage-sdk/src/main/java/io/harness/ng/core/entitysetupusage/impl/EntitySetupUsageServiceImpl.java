/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.entitysetupusage.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.EntitySetupUsageQueryFilterHelper;
import io.harness.ng.core.entitysetupusage.dto.EntityReferencesDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageBatchDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.dto.ReferredEntityDTO;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage.EntitySetupUsageKeys;
import io.harness.ng.core.entitysetupusage.mappers.EntitySetupUsageDTOtoEntity;
import io.harness.ng.core.entitysetupusage.mappers.EntitySetupUsageEntityToDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.repositories.entitysetupusage.EntitySetupUsageRepository;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
@OwnedBy(DX)
public class EntitySetupUsageServiceImpl implements EntitySetupUsageService {
  EntitySetupUsageQueryFilterHelper entitySetupUsageFilterHelper;
  EntitySetupUsageRepository entityReferenceRepository;
  EntitySetupUsageEntityToDTO setupUsageEntityToDTO;
  EntitySetupUsageDTOtoEntity entitySetupUsageDTOtoEntity;

  @Override
  public Page<EntitySetupUsageDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, EntityType referredEntityType, String searchTerm) {
    String referredEntityFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return listAllEntityUsage(page, size, accountIdentifier, referredEntityFQN, referredEntityType, searchTerm);
  }

  @Override
  public Page<EntitySetupUsageDTO> listAllEntityUsage(int page, int size, String accountIdentifier,
      String referredEntityFQN, EntityType referredEntityType, String searchTerm) {
    Criteria criteria = entitySetupUsageFilterHelper.createCriteriaFromEntityFilter(
        accountIdentifier, referredEntityFQN, referredEntityType, searchTerm);
    Pageable pageable = getPageRequest(page, size, Sort.by(Sort.Direction.DESC, EntitySetupUsageKeys.createdAt));
    Page<EntitySetupUsage> entityReferences = entityReferenceRepository.findAll(criteria, pageable);
    return entityReferences.map(entityReference -> setupUsageEntityToDTO.createEntityReferenceDTO(entityReference));
  }

  @Override
  public List<EntitySetupUsageDTO> listAllReferredUsages(int page, int size, String accountIdentifier,
      String referredByEntityFQN, EntityType referredEntityType, String searchTerm) {
    Criteria criteria = entitySetupUsageFilterHelper.createCriteriaForListAllReferredUsages(
        accountIdentifier, referredByEntityFQN, referredEntityType, searchTerm);
    Pageable pageable = getPageRequest(page, size, Sort.by(Sort.Direction.DESC, EntitySetupUsageKeys.createdAt));
    Page<EntitySetupUsage> entityReferences = entityReferenceRepository.findAll(criteria, pageable);
    List<EntitySetupUsage> entityReferencesContent = entityReferences.getContent();
    return entityReferencesContent.stream()
        .map(entityReference -> setupUsageEntityToDTO.createEntityReferenceDTO(entityReference))
        .collect(Collectors.toList());
  }

  @Override
  public Boolean isEntityReferenced(String accountIdentifier, String referredEntityFQN, EntityType referredEntityType) {
    Criteria criteria = entitySetupUsageFilterHelper.createCriteriaToCheckWhetherThisEntityIsReferred(
        accountIdentifier, referredEntityFQN, referredEntityType);
    return entityReferenceRepository.exists(criteria);
  }

  @Override
  public EntitySetupUsageDTO save(EntitySetupUsageDTO entitySetupUsageDTO) {
    EntitySetupUsage entitySetupUsage = entitySetupUsageDTOtoEntity.toEntityReference(entitySetupUsageDTO);
    EntitySetupUsage savedEntitySetupUsage = null;
    try {
      savedEntitySetupUsage = entityReferenceRepository.save(entitySetupUsage);
    } catch (DuplicateKeyException ex) {
      log.info(String.format("Error while saving the reference entity [%s]", ex.getMessage()));
      throw new DuplicateFieldException(
          String.format("Entity Reference already exists for entity [%s], referredBy [%s]",
              entitySetupUsage.getReferredEntityFQN(), entitySetupUsage.getReferredByEntityFQN()));
    }
    return setupUsageEntityToDTO.createEntityReferenceDTO(savedEntitySetupUsage);
  }

  @Override
  public Boolean delete(String accountIdentifier, String referredEntityFQN, EntityType referredEntityType,
      String referredByEntityFQN, EntityType referredByEntityType) {
    long numberOfRecordsDeleted = 0;
    numberOfRecordsDeleted =
        entityReferenceRepository
            .deleteByReferredEntityFQNAndReferredEntityTypeAndReferredByEntityFQNAndReferredByEntityTypeAndAccountIdentifier(
                referredEntityFQN, referredEntityType.toString(), referredByEntityFQN, referredByEntityType.toString(),
                accountIdentifier);
    log.info("Deleted {} records for the referred entity {}, referredBy {}", numberOfRecordsDeleted, referredEntityFQN,
        referredByEntityFQN);
    return numberOfRecordsDeleted > 0;
  }

  private Boolean deleteAllReferredByEntity(String accountIdentifier, String referredByEntityFQN,
      EntityType referredByEntityType, EntityType referredEntityType) {
    long numberOfRecordsDeleted = 0;
    Criteria criteria = entitySetupUsageFilterHelper.createCriteriaForDeletingAllReferredByEntries(
        accountIdentifier, referredByEntityFQN, referredByEntityType, referredEntityType);
    numberOfRecordsDeleted = entityReferenceRepository.delete(criteria);
    log.info("Deleted {} records for the referredBy entity {}", numberOfRecordsDeleted, referredByEntityFQN);
    return numberOfRecordsDeleted > 0;
  }

  private Pageable getPageRequest(int page, int size, Sort sort) {
    return PageRequest.of(page, size, sort);
  }

  // todo(abhinav): make delete and create a transactional operation
  @Override
  public Boolean flushSave(List<EntitySetupUsage> entitySetupUsage, EntityType entityTypeFromChannel,
      boolean deleteOldReferredByRecords, String accountId) {
    if (isEmpty(entitySetupUsage)) {
      return true;
    }
    if (deleteOldReferredByRecords) {
      deleteAllReferredByEntity(accountId,
          entitySetupUsage.get(0).getReferredByEntity().getEntityRef().getFullyQualifiedName(),
          entitySetupUsage.get(0).getReferredByEntity().getType(), entityTypeFromChannel);
    }
    final List<EntitySetupUsage> entitySetupUsageFiltered =
        filterSetupUsageByEntityTypes(entitySetupUsage, entityTypeFromChannel);
    return saveMultiple(entitySetupUsageFiltered);
  }

  @Override
  public EntityReferencesDTO listAllReferredUsagesBatch(String accountIdentifier, List<String> referredByEntityFQNList,
      EntityType referredByEntityType, EntityType referredEntityType) {
    Criteria criteria = entitySetupUsageFilterHelper.createCriteriaForListAllReferredUsagesBatch(
        accountIdentifier, referredByEntityFQNList, referredByEntityType, referredEntityType);
    long count = entityReferenceRepository.countAll(criteria);
    List<EntitySetupUsageDTO> entitySetupUsageDTOList = new ArrayList<>();
    int maxSizeQuery = 100;
    int page = 0;
    while (count > 0) {
      Pageable pageable =
          getPageRequest(page, maxSizeQuery, Sort.by(Sort.Direction.DESC, EntitySetupUsageKeys.createdAt));
      Page<EntitySetupUsage> entityReferences = entityReferenceRepository.findAll(criteria, pageable);
      List<EntitySetupUsage> entityReferencesContent = entityReferences.getContent();
      entitySetupUsageDTOList.addAll(
          entityReferencesContent.stream()
              .map(entityReference -> setupUsageEntityToDTO.createEntityReferenceDTO(entityReference))
              .collect(Collectors.toList()));
      count -= maxSizeQuery;
      page++;
    }
    Map<EntityDetail, List<EntitySetupUsageDTO>> entityDetailListMap =
        entitySetupUsageDTOList.stream().collect(Collectors.groupingBy(EntitySetupUsageDTO::getReferredByEntity));
    return EntityReferencesDTO.builder()
        .entitySetupUsageBatchList(buildEntitySetupUsageBatchDTOFromMap(entityDetailListMap))
        .build();
  }

  private List<EntitySetupUsageBatchDTO> buildEntitySetupUsageBatchDTOFromMap(
      Map<EntityDetail, List<EntitySetupUsageDTO>> entityDetailListMap) {
    List<EntitySetupUsageBatchDTO> entitySetupUsageBatchDTOList = new ArrayList<>();
    entityDetailListMap.forEach((referredByEntity, referredEntities)
                                    -> entitySetupUsageBatchDTOList.add(
                                        EntitySetupUsageBatchDTO.builder()
                                            .referredByEntity(referredByEntity.getEntityRef().getFullyQualifiedName())
                                            .referredEntities(referredEntities)
                                            .build()));
    return entitySetupUsageBatchDTOList;
  }

  private List<ReferredEntityDTO> getReferredEntityDTOList(List<EntitySetupUsageDTO> referredEntities) {
    List<ReferredEntityDTO> referredEntityDTOList = new ArrayList<>();
    referredEntities.forEach(referredEntity -> referredEntityDTOList.add(getReferredEntityDTO(referredEntity)));
    return referredEntityDTOList;
  }

  private ReferredEntityDTO getReferredEntityDTO(EntitySetupUsageDTO referredEntity) {
    return ReferredEntityDTO.builder().referredEntity(referredEntity.getReferredEntity()).build();
  }

  private Boolean saveMultiple(List<EntitySetupUsage> entitySetupUsages) {
    if (isEmpty(entitySetupUsages)) {
      return true;
    }

    entityReferenceRepository.saveAll(entitySetupUsages);
    List<String> referredEntitiesSaved = new ArrayList<>();
    log.info("Saved {} entities while saving referred By for entity {}", referredEntitiesSaved,
        entitySetupUsages.get(0).getReferredByEntity().getEntityRef().getFullyQualifiedName());
    return true;
  }

  public List<EntitySetupUsage> filterSetupUsageByEntityTypes(
      List<EntitySetupUsage> entitySetupUsages, EntityType entityTypeAllowed) {
    return EmptyPredicate.isEmpty(entitySetupUsages)
        ? Collections.emptyList()
        : entitySetupUsages.stream()
              .filter(entitySetupUsage -> {
                if (entitySetupUsage.getReferredEntity() != null) {
                  return entitySetupUsage.getReferredEntity().getType() == entityTypeAllowed;
                }
                return false;
              })
              .collect(Collectors.toList());
  }

  @Override
  public long deleteByReferredByEntityType(EntityType referredByEntityType) {
    return entityReferenceRepository.deleteAllByReferredByEntityType(referredByEntityType.getYamlName());
  }
}
