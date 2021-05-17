package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTimeConversionHelper;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sScaleRequest;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.supplier.ThrowingSupplier;

import com.google.inject.Inject;
import java.util.Optional;

@OwnedBy(CDP)
public class K8sScaleStep extends TaskExecutableWithRollback<K8sDeployResponse> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_SCALE.getYamlType()).build();

  public static final String K8S_SCALE_COMMAND_NAME = "Scale";
  @Inject private OutcomeService outcomeService;
  @Inject private K8sStepHelper k8sStepHelper;

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, StepElementParameters stepElementParameters, StepInputPackage inputPackage) {
    InfrastructureOutcome infrastructure = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));

    K8sScaleStepParameter scaleStepParameter = (K8sScaleStepParameter) stepElementParameters.getSpec();
    ParameterField<Integer> instances = K8sInstanceUnitType.Count == scaleStepParameter.getInstanceSelection().getType()
        ? ((CountInstanceSelection) scaleStepParameter.getInstanceSelection().getSpec()).getCount()
        : ((PercentageInstanceSelection) scaleStepParameter.getInstanceSelection().getSpec()).getPercentage();

    boolean skipSteadyCheck = scaleStepParameter.getSkipSteadyStateCheck() != null
        && scaleStepParameter.getSkipSteadyStateCheck().getValue() != null
        && scaleStepParameter.getSkipSteadyStateCheck().getValue();

    K8sScaleRequest request =
        K8sScaleRequest.builder()
            .commandName(K8S_SCALE_COMMAND_NAME)
            .releaseName(k8sStepHelper.getReleaseName(infrastructure))
            .instances(instances.getValue())
            .instanceUnitType(scaleStepParameter.getInstanceSelection().getType().getInstanceUnitType())
            .workload(scaleStepParameter.getWorkload().getValue())
            .maxInstances(Optional.empty()) // do we need those for scale?
            .skipSteadyStateCheck(skipSteadyCheck)
            .taskType(K8sTaskType.SCALE)
            .timeoutIntervalInMin(
                NGTimeConversionHelper.convertTimeStringToMinutes(stepElementParameters.getTimeout().getValue()))
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .build();

    return k8sStepHelper.queueK8sTask(stepElementParameters, request, ambiance, infrastructure).getTaskRequest();
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, StepElementParameters stepElementParameters,
      ThrowingSupplier<K8sDeployResponse> responseSupplier) throws Exception {
    K8sDeployResponse k8sTaskExecutionResponse = responseSupplier.get();
    // do we need to include the newPods with instance details + summaries
    StepResponseBuilder stepResponseBuilder =
        StepResponse.builder().unitProgressList(k8sTaskExecutionResponse.getCommandUnitsProgress().getUnitProgresses());

    if (k8sTaskExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return K8sStepHelper.getFailureResponseBuilder(k8sTaskExecutionResponse, stepResponseBuilder).build();
    }

    return stepResponseBuilder.status(Status.SUCCEEDED).build();
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }
}
