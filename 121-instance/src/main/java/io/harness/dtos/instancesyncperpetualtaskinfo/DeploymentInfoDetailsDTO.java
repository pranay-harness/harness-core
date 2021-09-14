/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.dtos.instancesyncperpetualtaskinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;

import lombok.Builder;
import lombok.Getter;

@OwnedBy(HarnessTeam.DX)
@Getter
@Builder
public class DeploymentInfoDetailsDTO {
  DeploymentInfoDTO deploymentInfoDTO;
  long lastUsedAt;

  public void setLastUsedAt(long lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }
}
