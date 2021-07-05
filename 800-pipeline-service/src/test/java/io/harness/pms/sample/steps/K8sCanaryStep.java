package io.harness.pms.sample.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.MapStepParameters;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class K8sCanaryStep implements SyncExecutable<MapStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType("k8sCanary").setStepCategory(StepCategory.STEP).build();

  @Override
  public Class<MapStepParameters> getStepParametersClass() {
    return MapStepParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, MapStepParameters stepParameters, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    log.info("K8s Canary Step parameters: {}", RecastOrchestrationUtils.toDocumentJson(stepParameters));
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
