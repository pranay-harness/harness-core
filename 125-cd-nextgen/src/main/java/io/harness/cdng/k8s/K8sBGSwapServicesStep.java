package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sSwapServiceSelectorsRequest;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.SkipTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;

import com.google.inject.Inject;

@OwnedBy(CDP)
public class K8sBGSwapServicesStep extends TaskExecutableWithRollback<K8sDeployResponse> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_BG_SWAP_SERVICES.getYamlType()).build();
  public static final String K8S_BG_SWAP_SERVICES_COMMAND_NAME = "Blue/Green Swap Services";
  public static final String SKIP_BG_SWAP_SERVICES_STEP_EXECUTION =
      "Services were not swapped in the forward phase. Skipping swapping in rollback.";
  public static final String BG_STEP_MISSING_ERROR = "Stage Deployment (Blue Green Deploy) is not configured";

  @Inject private K8sStepHelper k8sStepHelper;
  @Inject private OutcomeService outcomeService;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, StepElementParameters stepElementParameters, StepInputPackage inputPackage) {
    OptionalOutcome optionalOutcome = outcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_BG_SWAP_SERVICES_OUTCOME));
    boolean stepInRollbackSection = StepUtils.isStepInRollbackSection(ambiance);
    if (stepInRollbackSection && !optionalOutcome.isFound()) {
      return TaskRequest.newBuilder()
          .setSkipTaskRequest(SkipTaskRequest.newBuilder().setMessage(SKIP_BG_SWAP_SERVICES_STEP_EXECUTION).build())
          .build();
    }

    OptionalSweepingOutput optionalSweepingOutput = executionSweepingOutputService.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(OutcomeExpressionConstants.K8S_BLUE_GREEN_OUTCOME));
    if (!optionalSweepingOutput.isFound()) {
      throw new InvalidRequestException(BG_STEP_MISSING_ERROR, USER);
    }
    K8sBlueGreenOutcome k8sBlueGreenOutcome = (K8sBlueGreenOutcome) optionalSweepingOutput.getOutput();

    InfrastructureOutcome infrastructure = k8sStepHelper.getInfrastructureOutcome(ambiance);

    K8sSwapServiceSelectorsRequest swapServiceSelectorsRequest =
        K8sSwapServiceSelectorsRequest.builder()
            .service1(k8sBlueGreenOutcome.getPrimaryServiceName())
            .service2(k8sBlueGreenOutcome.getStageServiceName())
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .commandName(K8S_BG_SWAP_SERVICES_COMMAND_NAME)
            .taskType(K8sTaskType.SWAP_SERVICE_SELECTORS)
            .timeoutIntervalInMin(K8sStepHelper.getTimeoutInMin(stepElementParameters))
            .build();

    return k8sStepHelper.queueK8sTask(stepElementParameters, swapServiceSelectorsRequest, ambiance, infrastructure)
        .getTaskRequest();
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, StepElementParameters stepElementParameters,
      ThrowingSupplier<K8sDeployResponse> responseSupplier) throws Exception {
    K8sDeployResponse executionResponse = responseSupplier.get();
    StepResponseBuilder stepResponseBuilder =
        StepResponse.builder().unitProgressList(executionResponse.getCommandUnitsProgress().getUnitProgresses());

    if (executionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return stepResponseBuilder.status(Status.FAILED)
          .failureInfo(
              FailureInfo.newBuilder().setErrorMessage(K8sStepHelper.getErrorMessage(executionResponse)).build())
          .build();
    }

    // Save BGSwapServices Outcome only if you are in forward phase. We use this in rollback to check if we need to
    // run this step or not.
    if (!StepUtils.isStepInRollbackSection(ambiance)) {
      K8sBGSwapServicesOutcome bgSwapServicesOutcome = K8sBGSwapServicesOutcome.builder().build();
      stepResponseBuilder.stepOutcome(StepResponse.StepOutcome.builder()
                                          .name(OutcomeExpressionConstants.K8S_BG_SWAP_SERVICES_OUTCOME)
                                          .outcome(bgSwapServicesOutcome)
                                          .group(StepOutcomeGroup.STAGE.name())
                                          .build());
    }

    return stepResponseBuilder.status(Status.SUCCEEDED).build();
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }
}
