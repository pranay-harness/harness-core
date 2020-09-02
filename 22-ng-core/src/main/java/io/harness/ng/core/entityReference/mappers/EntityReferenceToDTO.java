package io.harness.ng.core.entityReference.mappers;

import com.google.inject.Singleton;

import io.harness.ng.EntityType;
import io.harness.ng.core.entityReference.dto.EntityReferenceDTO;
import io.harness.ng.core.entityReference.entity.EntityReference;

@Singleton
public class EntityReferenceToDTO {
  public EntityReferenceDTO createEntityReferenceDTO(EntityReference entityReference) {
    return EntityReferenceDTO.builder()
        .accountIdentifier(entityReference.getAccountIdentifier())
        .referredByEntityFQN(entityReference.getReferredByEntityFQN())
        .referredEntityFQN(entityReference.getReferredEntityFQN())
        .referredByEntityType(EntityType.valueOf(entityReference.getReferredByEntityType()))
        .referredEntityType(EntityType.valueOf(entityReference.getReferredEntityType()))
        .referredEntityName(entityReference.getReferredEntityName())
        .referredByEntityName(entityReference.getReferredByEntityName())
        .createdAt(entityReference.getCreatedAt())
        .build();
  }
}
