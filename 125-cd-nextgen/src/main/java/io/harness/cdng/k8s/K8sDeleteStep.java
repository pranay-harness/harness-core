package io.harness.cdng.k8s;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.task.k8s.K8sDeleteRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ngpipeline.common.AmbianceHelper;
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
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.LinkedList;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class K8sDeleteStep implements TaskExecutable<K8sDeleteStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_DELETE.getName()).build();
  public static final String K8S_DELETE_COMMAND_NAME = "Delete";

  @Inject private OutcomeService outcomeService;
  @Inject private K8sStepHelper k8sStepHelper;

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, K8sDeleteStepParameters stepParameters, StepInputPackage inputPackage) {
    ServiceOutcome serviceOutcome = (ServiceOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE));
    Map<String, ManifestOutcome> manifestOutcomeMap = serviceOutcome.getManifestResults();
    K8sManifestOutcome k8sManifestOutcome =
        k8sStepHelper.getK8sManifestOutcome(new LinkedList<>(manifestOutcomeMap.values()));
    StoreConfig storeConfig = k8sManifestOutcome.getStore();

    InfrastructureOutcome infrastructure = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));

    boolean isResourceName = DeleteResourcesType.ResourceName == stepParameters.getDeleteResources().getType();
    boolean isManifestFiles = DeleteResourcesType.ManifestPath == stepParameters.getDeleteResources().getType();

    String releaseName = k8sStepHelper.getReleaseName(infrastructure);
    final String accountId = AmbianceHelper.getAccountId(ambiance);

    K8sDeleteRequest request =
        K8sDeleteRequest.builder()
            .accountId(accountId)
            .releaseName(releaseName)
            .commandName(K8S_DELETE_COMMAND_NAME)
            .resources(
                isResourceName ? stepParameters.getDeleteResources().getSpec().getResources() : StringUtils.EMPTY)
            .deleteNamespacesForRelease(stepParameters.deleteResources.getSpec().getDeleteNamespace())
            .filePaths(
                isManifestFiles ? stepParameters.getDeleteResources().getSpec().getResources() : StringUtils.EMPTY)
            .taskType(K8sTaskType.DELETE)
            .timeoutIntervalInMin(K8sStepHelper.getTimeout(stepParameters))
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .manifestDelegateConfig(k8sStepHelper.getManifestDelegateConfig(storeConfig, ambiance))
            .build();

    return k8sStepHelper.queueK8sTask(stepParameters, request, ambiance, infrastructure).getTaskRequest();
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, K8sDeleteStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    ResponseData responseData = responseDataMap.values().iterator().next();
    K8sDeployResponse k8sTaskExecutionResponse = (K8sDeployResponse) responseData;

    if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      return StepResponse.builder().status(Status.SUCCEEDED).build();
    } else {
      return StepResponse.builder()
          .status(Status.FAILED)
          .failureInfo(FailureInfo.newBuilder().setErrorMessage(k8sTaskExecutionResponse.getErrorMessage()).build())
          .build();
    }
  }

  @Override
  public Class<K8sDeleteStepParameters> getStepParametersClass() {
    return K8sDeleteStepParameters.class;
  }
}
