package io.harness.ng.core.entityReference.mappers;

import com.google.inject.Singleton;

import io.harness.ng.core.entityReference.dto.EntityReferenceDTO;
import io.harness.ng.core.entityReference.entity.EntityReference;

@Singleton
public class EntityReferenceDTOtoEntity {
  public EntityReference toEntityReference(EntityReferenceDTO entityReferenceDTO) {
    return EntityReference.builder()
        .accountIdentifier(entityReferenceDTO.getAccountIdentifier())
        .referredByEntityFQN(entityReferenceDTO.getReferredByEntityFQN())
        .referredByEntityType(entityReferenceDTO.getReferredByEntityType().toString())
        .referredEntityFQN(entityReferenceDTO.getReferredEntityFQN())
        .referredEntityType(entityReferenceDTO.getReferredEntityType().toString())
        .build();
  }
}
