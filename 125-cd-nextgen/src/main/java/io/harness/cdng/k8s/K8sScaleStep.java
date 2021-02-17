package io.harness.cdng.k8s;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTimeConversionHelper;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sScaleRequest;
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
import io.harness.pms.yaml.ParameterField;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

public class K8sScaleStep implements TaskExecutable<K8sScaleStepParameter> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(ExecutionNodeType.K8S_SCALE.getName()).build();

  public static final String K8S_SCALE_COMMAND_NAME = "Scale";
  @Inject private OutcomeService outcomeService;
  @Inject private K8sStepHelper k8sStepHelper;

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, K8sScaleStepParameter stepParameters, StepInputPackage inputPackage) {
    ServiceOutcome serviceOutcome = (ServiceOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE));
    Map<String, ManifestOutcome> manifestOutcomeMap = serviceOutcome.getManifestResults();
    K8sManifestOutcome k8sManifestOutcome =
        k8sStepHelper.getK8sManifestOutcome(new LinkedList<>(manifestOutcomeMap.values()));
    StoreConfig storeConfig = k8sManifestOutcome.getStore();

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
            .manifestDelegateConfig(k8sStepHelper.getManifestDelegateConfig(storeConfig, ambiance))
            .build();

    return k8sStepHelper.queueK8sTask(stepParameters, request, ambiance, infrastructure).getTaskRequest();
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, K8sScaleStepParameter stepParameters, Map<String, ResponseData> responseDataMap) {
    ResponseData responseData = responseDataMap.values().iterator().next();
    K8sDeployResponse k8sTaskExecutionResponse = (K8sDeployResponse) responseData;
    // do we need to include the newPods with instance details + summaries
    if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      return StepResponse.builder().status(Status.SUCCEEDED).build();
    } else {
      StepResponseBuilder stepResponseBuilder =
          StepResponse.builder()
              .status(Status.FAILED)
              .failureInfo(
                  FailureInfo.newBuilder().setErrorMessage(k8sTaskExecutionResponse.getErrorMessage()).build());
      if (stepParameters.getRollbackInfo() != null) {
        stepResponseBuilder.stepOutcome(
            StepResponse.StepOutcome.builder()
                .name("RollbackOutcome")
                .outcome(RollbackOutcome.builder().rollbackInfo(stepParameters.getRollbackInfo()).build())
                .build());
      }
      return stepResponseBuilder.build();
    }
  }

  @Override
  public Class<K8sScaleStepParameter> getStepParametersClass() {
    return K8sScaleStepParameter.class;
  }
}
