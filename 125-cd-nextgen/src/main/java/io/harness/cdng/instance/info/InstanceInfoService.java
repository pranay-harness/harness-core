/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.instance.info;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.instance.outcome.DeploymentInfoOutcome;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.engine.outputs.SweepingOutputException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;

import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.CDP)
public interface InstanceInfoService {
  /**
   * List deployed service instances info.
   *
   * @param ambiance ambiance
   * @param stepType deployment step type
   * @return list of deployed server instances info or empty list if step type is not Instance sync step
   * @throws SweepingOutputException if step type supports Instance sync but sweeping output is not found
   */
  @NotNull List<ServerInstanceInfo> listServerInstances(Ambiance ambiance, StepType stepType);

  /**
   * Save list of deployed server instances info into sweeping output.
   *
   * @param ambiance ambiance
   * @param instanceInfoList server instances to be saved
   * @return step outcome
   */
  StepOutcome saveServerInstancesIntoSweepingOutput(
      Ambiance ambiance, @NotNull List<ServerInstanceInfo> instanceInfoList);

  /**
   * Save deployment info outcome directly into sweeping output.
   *
   * @param ambiance ambiance
   * @param deploymentInfoOutcome deployment info outcome
   * @return step outcome
   */
  StepOutcome saveDeploymentInfoOutcomeIntoSweepingOutput(
      Ambiance ambiance, DeploymentInfoOutcome deploymentInfoOutcome);
}
