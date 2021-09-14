/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.SyncExecutableResponse;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(PIPELINE)
@Slf4j
public class SyncStrategy implements ExecuteStrategy {
  @Inject private StepRegistry stepRegistry;
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;

  @Override
  public void start(InvokerPackage invokerPackage) {
    Ambiance ambiance = invokerPackage.getAmbiance();
    SyncExecutable syncExecutable = extractStep(ambiance);
    StepResponse stepResponse = syncExecutable.executeSync(ambiance, invokerPackage.getStepParameters(),
        invokerPackage.getInputPackage(), invokerPackage.getPassThroughData());

    sdkNodeExecutionService.handleStepResponse(ambiance.getPlanExecutionId(),
        AmbianceUtils.obtainCurrentRuntimeId(ambiance), StepResponseMapper.toStepResponseProto(stepResponse),
        ExecutableResponse.newBuilder()
            .setSync(SyncExecutableResponse.newBuilder()
                         .addAllLogKeys(syncExecutable.getLogKeys(ambiance))
                         .addAllUnits(syncExecutable.getCommandUnits(ambiance))
                         .build())
            .build());
  }

  @Override
  public SyncExecutable extractStep(Ambiance ambiance) {
    return (SyncExecutable) stepRegistry.obtain(AmbianceUtils.getCurrentStepType(ambiance));
  }
}
