package io.harness.cdng.k8s;

import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTimeConversionHelper;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.k8s.DeleteResourcesType;
import io.harness.delegate.task.k8s.K8sDeleteRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.SkipTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.execution.ErrorDataException;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.StepUtils;

import com.google.inject.Inject;
import java.util.function.Supplier;

@OwnedBy(HarnessTeam.CDP)
public class K8sCanaryDeleteStep implements TaskExecutable<K8sCanaryDeleteStepParameters, K8sDeployResponse> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_CANARY_DELETE.getYamlType()).build();
  public static final String K8S_CANARY_DELETE_COMMAND_NAME = "Canary Delete";
  public static final String SKIP_K8S_CANARY_DELETE_STEP_EXECUTION =
      "No canary workload was deployed in the forward phase. Skipping delete canary workload in rollback.";
  public static final String K8S_CANARY_DELETE_ALREADY_DELETED =
      "Canary workload has already been deleted. Skipping delete canary workload in rollback.";
  public static final String K8S_CANARY_STEP_MISSING = "Canary Deploy step is not configured.";

  @Inject private K8sStepHelper k8sStepHelper;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, K8sCanaryDeleteStepParameters stepParameters, StepInputPackage inputPackage) {
    OptionalSweepingOutput optionalSweepingOutput = executionSweepingOutputService.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(OutcomeExpressionConstants.K8S_CANARY_OUTCOME));

    if (!optionalSweepingOutput.isFound()) {
      throw new InvalidRequestException(K8S_CANARY_STEP_MISSING, USER);
    }

    K8sCanaryOutcome canaryOutcome = (K8sCanaryOutcome) optionalSweepingOutput.getOutput();
    if (StepUtils.isStepInRollbackSection(ambiance)) {
      if (!canaryOutcome.isCanaryWorkloadDeployed()) {
        return TaskRequest.newBuilder()
            .setSkipTaskRequest(SkipTaskRequest.newBuilder().setMessage(SKIP_K8S_CANARY_DELETE_STEP_EXECUTION).build())
            .build();
      }

      OptionalSweepingOutput existingCanaryDeleteOutput = executionSweepingOutputService.resolveOptional(
          ambiance, RefObjectUtils.getSweepingOutputRefObject(OutcomeExpressionConstants.K8S_CANARY_DELETE_OUTCOME));
      if (existingCanaryDeleteOutput.isFound()) {
        return TaskRequest.newBuilder()
            .setSkipTaskRequest(SkipTaskRequest.newBuilder().setMessage(K8S_CANARY_DELETE_ALREADY_DELETED).build())
            .build();
      }
    }

    InfrastructureOutcome infrastructure = k8sStepHelper.getInfrastructureOutcome(ambiance);

    K8sDeleteRequest request =
        K8sDeleteRequest.builder()
            .resources(canaryOutcome.getCanaryWorkload())
            .deleteResourcesType(DeleteResourcesType.ResourceName)
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
      Ambiance ambiance, K8sCanaryDeleteStepParameters stepParameters, Supplier<K8sDeployResponse> responseSupplier) {
    try {
      K8sDeployResponse k8sTaskExecutionResponse = responseSupplier.get();
      StepResponseBuilder responseBuilder = StepResponse.builder().unitProgressList(
          k8sTaskExecutionResponse.getCommandUnitsProgress().getUnitProgresses());

      if (k8sTaskExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
        return K8sStepHelper.getFailureResponseBuilder(stepParameters, k8sTaskExecutionResponse, responseBuilder)
            .build();
      }

      if (!StepUtils.isStepInRollbackSection(ambiance)) {
        executionSweepingOutputService.consume(ambiance, OutcomeExpressionConstants.K8S_CANARY_DELETE_OUTCOME,
            K8sCanaryDeleteOutcome.builder().build(), StepOutcomeGroup.STAGE.name());
      }

      return responseBuilder.status(Status.SUCCEEDED).build();
    } catch (ErrorDataException ex) {
      return K8sStepHelper
          .getDelegateErrorFailureResponseBuilder(stepParameters, (ErrorNotifyResponseData) ex.getErrorResponseData())
          .build();
    }
  }

  @Override
  public Class<K8sCanaryDeleteStepParameters> getStepParametersClass() {
    return K8sCanaryDeleteStepParameters.class;
  }
}
