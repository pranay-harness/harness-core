package io.harness.ng.core.entityReference;

import com.google.inject.Singleton;

import io.harness.EntityType;
import io.harness.ng.core.entityReference.dto.EntityReferenceDTO;

@Singleton
public class EntityReferenceHelper {
  public EntityReferenceDTO createEntityReference(String accountIdentifier, String referredEntityName,
      EntityType referredEntityType, String referredEntityFQN, String referredByEntityFQN, String referredByEntityName,
      EntityType referredByEntityType) {
    return EntityReferenceDTO.builder()
        .accountIdentifier(accountIdentifier)
        .referredEntityName(referredEntityName)
        .referredEntityType(referredEntityType)
        .referredEntityFQN(referredEntityFQN)
        .referredByEntityFQN(referredByEntityFQN)
        .referredByEntityName(referredByEntityName)
        .referredByEntityType(referredByEntityType)
        .build();
  }
}
