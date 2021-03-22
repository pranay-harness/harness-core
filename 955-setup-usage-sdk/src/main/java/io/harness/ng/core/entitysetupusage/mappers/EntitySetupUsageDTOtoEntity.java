package io.harness.ng.core.entitysetupusage.mappers;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class EntitySetupUsageDTOtoEntity {
  public EntitySetupUsage toEntityReference(EntitySetupUsageDTO entitySetupUsageDTO) {
    EntityDetail referredEntity = entitySetupUsageDTO.getReferredEntity();
    EntityDetail referredByEntity = entitySetupUsageDTO.getReferredByEntity();
    return EntitySetupUsage.builder()
        .accountIdentifier(entitySetupUsageDTO.getAccountIdentifier())
        .referredByEntity(referredByEntity)
        .referredByEntityFQN(referredByEntity.getEntityRef().getFullyQualifiedName())
        .referredByEntityType(referredByEntity.getType().toString())
        .referredEntityFQN(referredEntity.getEntityRef().getFullyQualifiedName())
        .referredEntityType(referredEntity.getType().toString())
        .referredEntity(referredEntity)
        .build();
  }
}
