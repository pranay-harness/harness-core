/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.k8s.beans.K8sExecutionPassThroughData;
import io.harness.cdng.k8s.beans.K8sRollingReleaseOutput;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTimeConversionHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.instancesync.mapper.K8sPodToServiceInstanceInfoMapper;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sRollingDeployRollbackResponse;
import io.harness.delegate.task.k8s.K8sRollingRollbackDeployRequest;
import io.harness.delegate.task.k8s.K8sRollingRollbackDeployRequest.K8sRollingRollbackDeployRequestBuilder;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.SkipTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.supplier.ThrowingSupplier;

import com.google.inject.Inject;

@OwnedBy(CDP)
public class K8sRollingRollbackStep extends TaskExecutableWithRollbackAndRbac<K8sDeployResponse> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.K8S_ROLLBACK_ROLLING.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  public static final String K8S_DEPLOYMENT_ROLLING_ROLLBACK_COMMAND_NAME = "Rolling Deployment Rollback";

  @Inject K8sStepHelper k8sStepHelper;
  @Inject private OutcomeService outcomeService;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private InstanceInfoService instanceInfoService;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    // Noop
  }

  @Override
  public TaskRequest obtainTaskAfterRbac(
      Ambiance ambiance, StepElementParameters stepElementParameters, StepInputPackage inputPackage) {
    K8sRollingRollbackStepParameters rollingRollbackStepParameters =
        (K8sRollingRollbackStepParameters) stepElementParameters.getSpec();
    if (EmptyPredicate.isEmpty(rollingRollbackStepParameters.getRollingStepFqn())) {
      return TaskRequest.newBuilder()
          .setSkipTaskRequest(SkipTaskRequest.newBuilder()
                                  .setMessage("K8s Rollout Deploy step was not executed. Skipping rollback.")
                                  .build())
          .build();
    }

    OptionalSweepingOutput k8sRollingReleaseOptionalOutput = executionSweepingOutputService.resolveOptional(ambiance,
        RefObjectUtils.getSweepingOutputRefObject(
            rollingRollbackStepParameters.getRollingStepFqn() + "." + K8sRollingReleaseOutput.OUTPUT_NAME));
    OptionalSweepingOutput k8sRollingOptionalOutput = executionSweepingOutputService.resolveOptional(ambiance,
        RefObjectUtils.getSweepingOutputRefObject(
            rollingRollbackStepParameters.getRollingStepFqn() + "." + OutcomeExpressionConstants.K8S_ROLL_OUT));

    if (!k8sRollingReleaseOptionalOutput.isFound()) {
      return TaskRequest.newBuilder()
          .setSkipTaskRequest(SkipTaskRequest.newBuilder()
                                  .setMessage("K8s Rollout Deploy step was not executed. Skipping rollback.")
                                  .build())
          .build();
    }

    K8sRollingRollbackDeployRequestBuilder rollbackRequestBuilder = K8sRollingRollbackDeployRequest.builder();
    InfrastructureOutcome infrastructure = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));

    if (k8sRollingOptionalOutput.isFound()) {
      K8sRollingOutcome k8sRollingOutcome = (K8sRollingOutcome) k8sRollingOptionalOutput.getOutput();
      rollbackRequestBuilder.releaseName(k8sRollingOutcome.getReleaseName())
          .releaseNumber(k8sRollingOutcome.getReleaseNumber());
    } else {
      K8sRollingReleaseOutput releaseOutput = (K8sRollingReleaseOutput) k8sRollingReleaseOptionalOutput.getOutput();
      rollbackRequestBuilder.releaseName(releaseOutput.getName());
    }

    rollbackRequestBuilder.commandName(K8S_DEPLOYMENT_ROLLING_ROLLBACK_COMMAND_NAME)
        .taskType(K8sTaskType.DEPLOYMENT_ROLLING_ROLLBACK)
        .timeoutIntervalInMin(
            NGTimeConversionHelper.convertTimeStringToMinutes(stepElementParameters.getTimeout().getValue()))
        .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
        .build();

    return k8sStepHelper
        .queueK8sTask(stepElementParameters, rollbackRequestBuilder.build(), ambiance,
            K8sExecutionPassThroughData.builder().infrastructure(infrastructure).build())
        .getTaskRequest();
  }

  @Override
  public StepResponse handleTaskResultWithSecurityContext(Ambiance ambiance,
      StepElementParameters stepElementParameters, ThrowingSupplier<K8sDeployResponse> responseSupplier)
      throws Exception {
    K8sDeployResponse executionResponse = responseSupplier.get();
    StepResponseBuilder stepResponseBuilder =
        StepResponse.builder().unitProgressList(executionResponse.getCommandUnitsProgress().getUnitProgresses());

    if (executionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return stepResponseBuilder.status(Status.FAILED)
          .failureInfo(
              FailureInfo.newBuilder().setErrorMessage(K8sStepHelper.getErrorMessage(executionResponse)).build())
          .build();
    }

    final K8sRollingDeployRollbackResponse response =
        (K8sRollingDeployRollbackResponse) executionResponse.getK8sNGTaskResponse();

    StepOutcome stepOutcome = instanceInfoService.saveServerInstancesIntoSweepingOutput(
        ambiance, K8sPodToServiceInstanceInfoMapper.toServerInstanceInfoList(response.getK8sPodList()));

    return stepResponseBuilder.status(Status.SUCCEEDED).stepOutcome(stepOutcome).build();
  }
}
