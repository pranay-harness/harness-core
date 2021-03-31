package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTimeConversionHelper;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sScaleRequest;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.execution.ErrorDataException;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;

import com.google.inject.Inject;
import java.util.Optional;
import java.util.function.Supplier;

@OwnedBy(PIPELINE)
public class K8sScaleStep implements TaskExecutable<K8sScaleStepParameter, K8sDeployResponse> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_SCALE.getYamlType()).build();

  public static final String K8S_SCALE_COMMAND_NAME = "Scale";
  @Inject private OutcomeService outcomeService;
  @Inject private K8sStepHelper k8sStepHelper;

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, K8sScaleStepParameter stepParameters, StepInputPackage inputPackage) {
    InfrastructureOutcome infrastructure = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));

    ParameterField<Integer> instances = K8sInstanceUnitType.Count == stepParameters.getInstanceSelection().getType()
        ? ((CountInstanceSelection) stepParameters.getInstanceSelection().getSpec()).getCount()
        : ((PercentageInstanceSelection) stepParameters.getInstanceSelection().getSpec()).getPercentage();

    boolean skipSteadyCheck = stepParameters.getSkipSteadyStateCheck() != null
        && stepParameters.getSkipSteadyStateCheck().getValue() != null
        && stepParameters.getSkipSteadyStateCheck().getValue();

    K8sScaleRequest request =
        K8sScaleRequest.builder()
            .commandName(K8S_SCALE_COMMAND_NAME)
            .releaseName(k8sStepHelper.getReleaseName(infrastructure))
            .instances(instances.getValue())
            .instanceUnitType(stepParameters.getInstanceSelection().getType().getInstanceUnitType())
            .workload(stepParameters.getWorkload().getValue())
            .maxInstances(Optional.empty()) // do we need those for scale?
            .skipSteadyStateCheck(skipSteadyCheck)
            .taskType(K8sTaskType.SCALE)
            .timeoutIntervalInMin(
                NGTimeConversionHelper.convertTimeStringToMinutes(stepParameters.getTimeout().getValue()))
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .build();

    return k8sStepHelper.queueK8sTask(stepParameters, request, ambiance, infrastructure).getTaskRequest();
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, K8sScaleStepParameter stepParameters, Supplier<K8sDeployResponse> responseSupplier) {
    try {
      K8sDeployResponse k8sTaskExecutionResponse = responseSupplier.get();
      // do we need to include the newPods with instance details + summaries
      StepResponseBuilder stepResponseBuilder = StepResponse.builder().unitProgressList(
          k8sTaskExecutionResponse.getCommandUnitsProgress().getUnitProgresses());

      if (k8sTaskExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
        return K8sStepHelper.getFailureResponseBuilder(stepParameters, k8sTaskExecutionResponse, stepResponseBuilder)
            .build();
      }

      return stepResponseBuilder.status(Status.SUCCEEDED).build();
    } catch (ErrorDataException ex) {
      return K8sStepHelper
          .getDelegateErrorFailureResponseBuilder(stepParameters, (ErrorNotifyResponseData) ex.getErrorResponseData())
          .build();
    }
  }

  @Override
  public Class<K8sScaleStepParameter> getStepParametersClass() {
    return K8sScaleStepParameter.class;
  }
}
