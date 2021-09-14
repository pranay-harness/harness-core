/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.models;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.dtos.DeploymentSummaryDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@OwnedBy(HarnessTeam.DX)
@EqualsAndHashCode(callSuper = false)
public class DeploymentEvent {
  private DeploymentSummaryDTO deploymentSummaryDTO;
  private RollbackInfo rollbackInfo;
  private InfrastructureOutcome infrastructureOutcome;
}
