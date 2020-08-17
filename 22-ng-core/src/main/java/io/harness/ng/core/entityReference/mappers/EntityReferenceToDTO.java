package io.harness.ng.core.entityReference.mappers;

import com.google.inject.Singleton;

import io.harness.ng.core.entityReference.ReferenceEntityType;
import io.harness.ng.core.entityReference.dto.EntityReferenceDTO;
import io.harness.ng.core.entityReference.entity.EntityReference;

@Singleton
public class EntityReferenceToDTO {
  public EntityReferenceDTO createEntityReferenceDTO(EntityReference entityReference) {
    return EntityReferenceDTO.builder()
        .accountIdentifier(entityReference.getAccountIdentifier())
        .referredByEntityFQN(entityReference.getReferredByEntityFQN())
        .referredEntityFQN(entityReference.getReferredEntityFQN())
        .referredByEntityType(ReferenceEntityType.valueOf(entityReference.getReferredByEntityType()))
        .referredEntityType(ReferenceEntityType.valueOf(entityReference.getReferredEntityType()))
        .build();
  }
}
