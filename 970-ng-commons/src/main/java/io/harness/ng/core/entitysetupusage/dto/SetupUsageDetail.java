/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.entitysetupusage.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@OwnedBy(DX)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "type")
@JsonSubTypes({
  @JsonSubTypes.
  Type(value = SecretReferredByConnectorSetupUsageDetail.class, name = "SecretReferredByConnectorSetupUsageDetail")
  ,
      @JsonSubTypes.Type(
          value = EntityReferredByPipelineSetupUsageDetail.class, name = "EntityReferredByPipelineSetupUsageDetail")
})
public interface SetupUsageDetail {}
