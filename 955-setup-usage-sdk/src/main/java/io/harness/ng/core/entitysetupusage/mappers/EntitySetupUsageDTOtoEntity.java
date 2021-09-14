/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.entitysetupusage.mappers;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.dto.SetupUsageDetail;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class EntitySetupUsageDTOtoEntity {
  public EntitySetupUsage toEntityReference(EntitySetupUsageDTO entitySetupUsageDTO) {
    EntityDetail referredEntity = entitySetupUsageDTO.getReferredEntity();
    EntityDetail referredByEntity = entitySetupUsageDTO.getReferredByEntity();
    EntityReference referredByEntityRef = referredByEntity.getEntityRef();
    EntityReference referredEntityRef = referredEntity.getEntityRef();
    SetupUsageDetail setupUsageDetail = entitySetupUsageDTO.getDetail();
    return EntitySetupUsage.builder()
        .accountIdentifier(entitySetupUsageDTO.getAccountIdentifier())
        .referredByEntity(referredByEntity)
        .referredByEntityFQN(referredByEntityRef.getFullyQualifiedName())
        .referredByEntityType(referredByEntity.getType().toString())
        .referredByEntityRepoIdentifier(referredByEntityRef.getRepoIdentifier())
        .referredByEntityBranch(referredByEntityRef.getBranch())
        .referredByEntityIsDefault(checkWhetherDefaultEntity(referredByEntityRef))
        .referredEntityFQN(referredEntityRef.getFullyQualifiedName())
        .referredEntityType(referredEntity.getType().toString())
        .referredEntity(referredEntity)
        .referredEntityRepoIdentifier(referredEntityRef.getRepoIdentifier())
        .referredEntityBranch(referredEntityRef.getBranch())
        .referredEntityIsDefault(checkWhetherDefaultEntity(referredEntityRef))
        .detail(setupUsageDetail)
        .build();
  }

  private Boolean checkWhetherDefaultEntity(EntityReference referredByEntityRef) {
    if (referredByEntityRef.getBranch() == null && referredByEntityRef.getRepoIdentifier() == null) {
      return true;
    }
    return referredByEntityRef.isDefault();
  }
}
