/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.entitysetupusage.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;

import lombok.extern.slf4j.Slf4j;

@OwnedBy(DX)
@Slf4j
public enum SetupUsageDetailType {
  SECRET_REFERRED_BY_CONNECTOR,
  CONNECTOR_REFERRED_BY_PIPELINE,
  ENVIRONMENT_REFERRED_BY_PIPELINE,
  SERVICE_REFERRED_BY_PIPELINE,
  SECRET_REFERRED_BY_PIPELINE;

  public static boolean isReferredByPipeline(String setupUsageDetailType) {
    SetupUsageDetailType type = SetupUsageDetailType.valueOf(setupUsageDetailType);
    switch (type) {
      case CONNECTOR_REFERRED_BY_PIPELINE:
      case SECRET_REFERRED_BY_PIPELINE:
      case SERVICE_REFERRED_BY_PIPELINE:
      case ENVIRONMENT_REFERRED_BY_PIPELINE:
        return true;
      default:
        return false;
    }
  }

  public static SetupUsageDetailType getTypeFromEntityTypeProtoEnumName(String entityTypeProtoEnumName) {
    if (EntityTypeProtoEnum.ENVIRONMENT.name().equals(entityTypeProtoEnumName)) {
      return ENVIRONMENT_REFERRED_BY_PIPELINE;
    } else if (EntityTypeProtoEnum.SERVICE.name().equals(entityTypeProtoEnumName)) {
      return SERVICE_REFERRED_BY_PIPELINE;
    } else if (EntityTypeProtoEnum.CONNECTORS.name().equals(entityTypeProtoEnumName)) {
      return CONNECTOR_REFERRED_BY_PIPELINE;
    } else if (EntityTypeProtoEnum.SECRETS.name().equals(entityTypeProtoEnumName)) {
      return SECRET_REFERRED_BY_PIPELINE;
    }
    return null;
  }
}
