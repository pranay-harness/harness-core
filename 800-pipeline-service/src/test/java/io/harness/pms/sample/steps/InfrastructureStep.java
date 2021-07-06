package io.harness.pms.sample.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class InfrastructureStep implements SyncExecutable<InfrastructureStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType("infrastructure").setStepCategory(StepCategory.STEP).build();

  @Override
  public Class<InfrastructureStepParameters> getStepParametersClass() {
    return InfrastructureStepParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, InfrastructureStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    log.info("Infrastructure Step parameters: {}", RecastOrchestrationUtils.toJson(stepParameters));
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
