package io.harness.cdng.k8s;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTimeConversionHelper;
import io.harness.delegate.task.k8s.K8sDeleteRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
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
import io.harness.pms.sdk.core.steps.io.RollbackOutcome;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;

public class K8sCanaryDeleteStep implements TaskExecutable<K8sCanaryDeleteStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_CANARY_DELETE.getYamlType()).build();
  public static final String K8S_CANARY_DELETE_COMMAND_NAME = "Canary Delete";

  @Inject private OutcomeService outcomeService;
  @Inject private K8sStepHelper k8sStepHelper;

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, K8sCanaryDeleteStepParameters stepParameters, StepInputPackage inputPackage) {
    K8sCanaryOutcome canaryOutcome = (K8sCanaryOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_CANARY_OUTCOME));
    InfrastructureOutcome infrastructure = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));

    K8sDeleteRequest request =
        K8sDeleteRequest.builder()
            .resources(canaryOutcome.getCanaryWorkload())
            .commandName(K8S_CANARY_DELETE_COMMAND_NAME)
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .deleteNamespacesForRelease(false)
            .taskType(K8sTaskType.DELETE)
            .timeoutIntervalInMin(
                NGTimeConversionHelper.convertTimeStringToMinutes(stepParameters.getTimeout().getValue()))
            .build();

    return k8sStepHelper.queueK8sTask(stepParameters, request, ambiance, infrastructure).getTaskRequest();
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, K8sCanaryDeleteStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    K8sDeployResponse k8sTaskExecutionResponse = (K8sDeployResponse) responseDataMap.values().iterator().next();
    StepResponseBuilder responseBuilder = StepResponse.builder();

    if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      responseBuilder.status(Status.SUCCEEDED);
    } else {
      responseBuilder.status(Status.FAILED);
      responseBuilder.failureInfo(
          FailureInfo.newBuilder().setErrorMessage(k8sTaskExecutionResponse.getErrorMessage()).build());
      if (stepParameters.getRollbackInfo() != null) {
        responseBuilder.stepOutcome(
            StepResponse.StepOutcome.builder()
                .name("RollbackOutcome")
                .outcome(RollbackOutcome.builder().rollbackInfo(stepParameters.getRollbackInfo()).build())
                .build());
      }
    }

    return responseBuilder.build();
  }

  @Override
  public Class<K8sCanaryDeleteStepParameters> getStepParametersClass() {
    return K8sCanaryDeleteStepParameters.class;
  }
}
