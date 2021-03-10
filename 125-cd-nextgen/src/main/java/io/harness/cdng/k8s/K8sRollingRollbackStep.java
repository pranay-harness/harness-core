package io.harness.cdng.k8s;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTimeConversionHelper;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sRollingRollbackDeployRequest;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.SkipTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.tasks.ResponseData;

import software.wings.sm.states.k8s.K8sRollingDeployRollback;

import com.google.inject.Inject;
import java.util.Map;

public class K8sRollingRollbackStep implements TaskExecutable<K8sRollingRollbackStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_ROLLBACK_ROLLING.getYamlType()).build();

  @Inject K8sStepHelper k8sStepHelper;
  @Inject private OutcomeService outcomeService;

  @Override
  public Class<K8sRollingRollbackStepParameters> getStepParametersClass() {
    return K8sRollingRollbackStepParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, K8sRollingRollbackStepParameters stepParameters, StepInputPackage inputPackage) {
    OptionalOutcome optionalOutcome = outcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_ROLL_OUT));

    if (!optionalOutcome.isFound()) {
      return TaskRequest.newBuilder()
          .setSkipTaskRequest(SkipTaskRequest.newBuilder()
                                  .setMessage("K8s Rollout Deploy step was not executed. Skipping rollback.")
                                  .build())
          .build();
    }

    K8sRollingOutcome k8sRollingOutcome = (K8sRollingOutcome) optionalOutcome.getOutcome();
    InfrastructureOutcome infrastructure = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));

    K8sRollingRollbackDeployRequest rollingRollbackDeployRequest =
        K8sRollingRollbackDeployRequest.builder()
            .releaseName(k8sRollingOutcome.getReleaseName())
            .releaseNumber(k8sRollingOutcome.getReleaseNumber())
            .commandName(K8sRollingDeployRollback.K8S_DEPLOYMENT_ROLLING_ROLLBACK_COMMAND_NAME)
            .taskType(K8sTaskType.DEPLOYMENT_ROLLING_ROLLBACK)
            .timeoutIntervalInMin(
                NGTimeConversionHelper.convertTimeStringToMinutes(stepParameters.getTimeout().getValue()))
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .build();

    return k8sStepHelper.queueK8sTask(stepParameters, rollingRollbackDeployRequest, ambiance, infrastructure)
        .getTaskRequest();
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, K8sRollingRollbackStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    ResponseData responseData = responseDataMap.values().iterator().next();
    if (responseData instanceof ErrorNotifyResponseData) {
      return K8sStepHelper
          .getDelegateErrorFailureResponseBuilder(stepParameters, (ErrorNotifyResponseData) responseData)
          .build();
    }

    K8sDeployResponse executionResponse = (K8sDeployResponse) responseData;
    StepResponseBuilder stepResponseBuilder =
        StepResponse.builder().unitProgressList(executionResponse.getCommandUnitsProgress().getUnitProgresses());

    if (executionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return stepResponseBuilder.status(Status.FAILED)
          .failureInfo(
              FailureInfo.newBuilder().setErrorMessage(K8sStepHelper.getErrorMessage(executionResponse)).build())
          .build();
    }

    return stepResponseBuilder.status(Status.SUCCEEDED).build();
  }
}
