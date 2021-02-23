package io.harness.cdng.k8s;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sSwapServiceSelectorsRequest;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;

public class K8sBGSwapServicesStep implements TaskExecutable<K8sBGSwapServicesStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_BG_SWAP_SERVICES.getYamlType()).build();
  public static final String K8S_BG_SWAP_SERVICES_COMMAND_NAME = "Blue/Green Swap Services";

  @Inject private K8sStepHelper k8sStepHelper;
  @Inject private OutcomeService outcomeService;

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, K8sBGSwapServicesStepParameters stepParameters, StepInputPackage inputPackage) {
    K8sBlueGreenOutcome k8sBlueGreenOutcome = (K8sBlueGreenOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_BLUE_GREEN_OUTCOME));
    InfrastructureOutcome infrastructure = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));

    K8sSwapServiceSelectorsRequest swapServiceSelectorsRequest =
        K8sSwapServiceSelectorsRequest.builder()
            .service1(k8sBlueGreenOutcome.getPrimaryServiceName())
            .service2(k8sBlueGreenOutcome.getStageServiceName())
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .commandName(K8S_BG_SWAP_SERVICES_COMMAND_NAME)
            .taskType(K8sTaskType.SWAP_SERVICE_SELECTORS)
            .timeoutIntervalInMin(K8sStepHelper.getTimeout(stepParameters))
            .build();

    return k8sStepHelper.queueK8sTask(stepParameters, swapServiceSelectorsRequest, ambiance, infrastructure)
        .getTaskRequest();
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, K8sBGSwapServicesStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    K8sDeployResponse executionResponse = (K8sDeployResponse) responseDataMap.values().iterator().next();

    StepResponseBuilder stepResponseBuilder =
        StepResponse.builder().unitProgressList(executionResponse.getCommandUnitsProgress().getUnitProgresses());
    if (executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      return stepResponseBuilder.status(Status.SUCCEEDED).build();
    } else {
      return stepResponseBuilder.status(Status.FAILED)
          .failureInfo(
              FailureInfo.newBuilder().setErrorMessage(K8sStepHelper.getErrorMessage(executionResponse)).build())
          .build();
    }
  }

  @Override
  public Class<K8sBGSwapServicesStepParameters> getStepParametersClass() {
    return K8sBGSwapServicesStepParameters.class;
  }
}
