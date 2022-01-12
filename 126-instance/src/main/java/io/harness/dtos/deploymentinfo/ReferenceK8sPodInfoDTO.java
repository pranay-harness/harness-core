/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.dtos.deploymentinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.DX)
// Created for reference, either change its name before modifying or create a new deployment info
public class ReferenceK8sPodInfoDTO extends DeploymentInfoDTO {
  String podName;

  @Override
  public String getType() {
    return null;
  }

  @Override
  public String prepareInstanceSyncHandlerKey() {
    return podName;
  }
}
