/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.entitysetupusage.mappers;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.dto.SetupUsageDetail;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class EntitySetupUsageEntityToDTO {
  public EntitySetupUsageDTO createEntityReferenceDTO(EntitySetupUsage entitySetupUsage) {
    EntityDetail referredEntity = entitySetupUsage.getReferredEntity();
    EntityDetail referredByEntity = entitySetupUsage.getReferredByEntity();
    SetupUsageDetail setupUsageDetail = entitySetupUsage.getDetail();
    return EntitySetupUsageDTO.builder()
        .accountIdentifier(entitySetupUsage.getAccountIdentifier())
        .referredEntity(referredEntity)
        .referredByEntity(referredByEntity)
        .detail(setupUsageDetail)
        .createdAt(entitySetupUsage.getCreatedAt())
        .build();
  }
}
