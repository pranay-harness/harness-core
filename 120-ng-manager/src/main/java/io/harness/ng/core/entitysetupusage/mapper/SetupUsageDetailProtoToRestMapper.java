/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.entitysetupusage.mapper;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.ng.core.entitysetupusage.dto.SetupUsageDetailType.SECRET_REFERRED_BY_CONNECTOR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entitysetupusage.EntityDetailWithSetupUsageDetailProtoDTO;
import io.harness.ng.core.entitysetupusage.dto.EntityReferredByPipelineSetupUsageDetail;
import io.harness.ng.core.entitysetupusage.dto.SecretReferredByConnectorSetupUsageDetail;
import io.harness.ng.core.entitysetupusage.dto.SetupUsageDetail;
import io.harness.ng.core.entitysetupusage.dto.SetupUsageDetailType;

import com.google.inject.Singleton;

@OwnedBy(DX)
@Singleton
public class SetupUsageDetailProtoToRestMapper {
  public SetupUsageDetail toRestDTO(EntityDetailWithSetupUsageDetailProtoDTO setupUsageDetailProtoDTO) {
    if (setupUsageDetailProtoDTO == null) {
      return null;
    }
    if (SECRET_REFERRED_BY_CONNECTOR.toString().equals(setupUsageDetailProtoDTO.getType())) {
      return SecretReferredByConnectorSetupUsageDetail.builder()
          .fieldName(setupUsageDetailProtoDTO.getSecretConnectorDetail().getFieldName())
          .build();
    }

    if (SetupUsageDetailType.isReferredByPipeline(setupUsageDetailProtoDTO.getType())) {
      return EntityReferredByPipelineSetupUsageDetail.builder()
          .identifier(setupUsageDetailProtoDTO.getEntityInPipelineDetail().getIdentifier())
          .referenceType(String.valueOf(setupUsageDetailProtoDTO.getEntityInPipelineDetail().getType()))
          .build();
    }
    return null;
  }
}
