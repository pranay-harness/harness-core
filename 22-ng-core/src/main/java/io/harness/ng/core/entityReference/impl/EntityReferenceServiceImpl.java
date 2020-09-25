package io.harness.ng.core.entityReference.impl;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.entityReference.EntityReferenceFilterHelper;
import io.harness.ng.core.entityReference.dto.EntityReferenceDTO;
import io.harness.ng.core.entityReference.entity.EntityReference;
import io.harness.ng.core.entityReference.entity.EntityReference.EntityReferenceKeys;
import io.harness.ng.core.entityReference.mappers.EntityReferenceDTOtoEntity;
import io.harness.ng.core.entityReference.mappers.EntityReferenceToDTO;
import io.harness.ng.core.entityReference.repositories.EntityReferenceRepository;
import io.harness.ng.core.entityReference.service.EntityReferenceService;
import io.harness.utils.FullyQualifiedIdentifierHelper;
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
public class EntityReferenceServiceImpl implements EntityReferenceService {
  EntityReferenceFilterHelper entityReferenceFilterHelper;
  EntityReferenceRepository entityReferenceRepository;
  EntityReferenceToDTO entityReferenceToDTO;
  EntityReferenceDTOtoEntity entityReferenceDTOtoEntity;

  @Override
  public Page<EntityReferenceDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, String searchTerm) {
    Criteria criteria = entityReferenceFilterHelper.createCriteriaFromEntityFilter(
        accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier, searchTerm);
    Pageable pageable = getPageRequest(page, size, Sort.by(Sort.Direction.DESC, EntityReferenceKeys.createdAt));
    Page<EntityReference> entityReferences = entityReferenceRepository.findAll(criteria, pageable);
    return entityReferences.map(entityReference -> entityReferenceToDTO.createEntityReferenceDTO(entityReference));
  }

  @Override
  public Boolean isEntityReferenced(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    String referrerdEntityFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return entityReferenceRepository.existsByReferredEntityFQN(referrerdEntityFQN);
  }

  @Override
  public EntityReferenceDTO save(EntityReferenceDTO entityReferenceDTO) {
    EntityReference entityReference = entityReferenceDTOtoEntity.toEntityReference(entityReferenceDTO);
    EntityReference savedEntityReference = null;
    try {
      savedEntityReference = entityReferenceRepository.save(entityReference);
    } catch (DuplicateKeyException ex) {
      logger.info(String.format("Error while saving the reference entity [%s]", ex.getMessage()));
      throw new DuplicateFieldException(format("Entity Reference already exists for entity [%s], referredBy [%s]",
          entityReference.getReferredEntityFQN(), entityReference.getReferredByEntityFQN()));
    }
    return entityReferenceToDTO.createEntityReferenceDTO(savedEntityReference);
  }

  @Override
  public Boolean delete(String referredEntityFQN, String referredByEntityFQN) {
    long numberOfRecordsDeleted = entityReferenceRepository.deleteByReferredEntityFQNAndReferredByEntityFQN(
        referredEntityFQN, referredByEntityFQN);
    return numberOfRecordsDeleted == 1;
  }

  private Pageable getPageRequest(int page, int size, Sort sort) {
    return PageRequest.of(page, size, sort);
  }
}
