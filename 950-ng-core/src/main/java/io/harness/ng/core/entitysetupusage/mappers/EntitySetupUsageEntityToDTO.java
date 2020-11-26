package io.harness.ng.core.entitysetupusage.mappers;

import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import com.google.inject.Singleton;

@Singleton
public class EntitySetupUsageEntityToDTO {
  public EntitySetupUsageDTO createEntityReferenceDTO(EntitySetupUsage entitySetupUsage) {
    EntityDetail referredEntity = entitySetupUsage.getReferredEntity();
    EntityDetail referredByEntity = entitySetupUsage.getReferredByEntity();
    return EntitySetupUsageDTO.builder()
        .accountIdentifier(entitySetupUsage.getAccountIdentifier())
        .referredEntity(referredEntity)
        .referredByEntity(referredByEntity)
        .createdAt(entitySetupUsage.getCreatedAt())
        .build();
  }
}
