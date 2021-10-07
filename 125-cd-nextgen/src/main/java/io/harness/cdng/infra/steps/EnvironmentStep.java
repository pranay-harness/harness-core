package io.harness.cdng.infra.steps;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.InfraStepUtils;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.OutputExpressionConstants;
import io.harness.steps.environment.EnvironmentOutcome;
import io.harness.steps.executable.SyncExecutableWithRbac;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class EnvironmentStep implements SyncExecutableWithRbac<InfraSectionStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.ENVIRONMENT.getName()).setStepCategory(StepCategory.STEP).build();

  @Inject @Named("PRIVILEGED") private AccessControlClient accessControlClient;
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private EnvironmentService environmentService;

  @Override
  public void validateResources(Ambiance ambiance, InfraSectionStepParameters stepParameters) {
    InfraStepUtils.validateResources(accessControlClient, ambiance, stepParameters);
  }

  @Override
  public StepResponse executeSyncAfterRbac(Ambiance ambiance, InfraSectionStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    log.info("Starting execution for InfraSection Step [{}]", stepParameters);
    EnvironmentOutcome environmentOutcome = InfraStepUtils.processEnvironment(environmentService, ambiance,
        stepParameters.getUseFromStage(), stepParameters.getEnvironment(), stepParameters.getEnvironmentRef());
    executionSweepingOutputResolver.consume(
        ambiance, OutputExpressionConstants.ENVIRONMENT, environmentOutcome, StepOutcomeGroup.STAGE.name());
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  @Override
  public Class<InfraSectionStepParameters> getStepParametersClass() {
    return InfraSectionStepParameters.class;
  }
}
