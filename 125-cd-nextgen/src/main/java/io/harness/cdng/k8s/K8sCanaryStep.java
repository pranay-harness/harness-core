package io.harness.cdng.k8s;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.k8s.K8sCanaryDeployRequest;
import io.harness.delegate.task.k8s.K8sCanaryDeployResponse;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.TaskChainExecutable;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.StepOutcomeGroup;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;

public class K8sCanaryStep implements TaskChainExecutable<K8sCanaryStepParameters>, K8sStepExecutor {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_CANARY.getYamlType()).build();
  private final String K8S_CANARY_DEPLOY_COMMAND_NAME = "Canary Deployment";

  @Inject private K8sStepHelper k8sStepHelper;

  @Override
  public TaskChainResponse startChainLink(
      Ambiance ambiance, K8sCanaryStepParameters stepParameters, StepInputPackage inputPackage) {
    validate(stepParameters);
    return k8sStepHelper.startChainLink(this, ambiance, stepParameters);
  }

  @Override
  public TaskChainResponse executeNextLink(Ambiance ambiance, K8sCanaryStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    return k8sStepHelper.executeNextLink(this, ambiance, stepParameters, passThroughData, responseDataMap);
  }

  @Override
  public TaskChainResponse executeK8sTask(ManifestOutcome k8sManifestOutcome, Ambiance ambiance,
      K8sStepParameters stepParameters, List<String> valuesFileContents, InfrastructureOutcome infrastructure) {
    final String releaseName = k8sStepHelper.getReleaseName(infrastructure);
    final K8sCanaryStepParameters canaryStepParameters = (K8sCanaryStepParameters) stepParameters;
    final Integer instancesValue = canaryStepParameters.getInstanceSelection().getSpec().getInstances();
    final String accountId = AmbianceHelper.getAccountId(ambiance);
    final boolean skipDryRun =
        !ParameterField.isNull(stepParameters.getSkipDryRun()) && stepParameters.getSkipDryRun().getValue();

    K8sCanaryDeployRequest k8sCanaryDeployRequest =
        K8sCanaryDeployRequest.builder()
            .skipDryRun(skipDryRun)
            .releaseName(releaseName)
            .commandName(K8S_CANARY_DEPLOY_COMMAND_NAME)
            .taskType(K8sTaskType.CANARY_DEPLOY)
            .instanceUnitType(canaryStepParameters.getInstanceSelection().getType().getInstanceUnitType())
            .instances(instancesValue)
            .timeoutIntervalInMin(K8sStepHelper.getTimeout(stepParameters))
            .valuesYamlList(k8sStepHelper.renderValues(ambiance, valuesFileContents))
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .manifestDelegateConfig(k8sStepHelper.getManifestDelegateConfig(k8sManifestOutcome, ambiance))
            .accountId(accountId)
            .build();
    return k8sStepHelper.queueK8sTask(stepParameters, k8sCanaryDeployRequest, ambiance, infrastructure);
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, K8sCanaryStepParameters stepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    if (passThroughData instanceof GitFetchResponsePassThroughData) {
      return k8sStepHelper.handleGitTaskFailure((GitFetchResponsePassThroughData) passThroughData);
    }

    ResponseData responseData = responseDataMap.values().iterator().next();
    if (responseData instanceof ErrorNotifyResponseData) {
      return K8sStepHelper
          .getDelegateErrorFailureResponseBuilder(stepParameters, (ErrorNotifyResponseData) responseData)
          .build();
    }

    K8sDeployResponse k8sTaskExecutionResponse = (K8sDeployResponse) responseData;
    StepResponseBuilder responseBuilder =
        StepResponse.builder().unitProgressList(k8sTaskExecutionResponse.getCommandUnitsProgress().getUnitProgresses());

    if (k8sTaskExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return K8sStepHelper.getFailureResponseBuilder(stepParameters, k8sTaskExecutionResponse, responseBuilder).build();
    }

    InfrastructureOutcome infrastructure = (InfrastructureOutcome) passThroughData;
    K8sCanaryDeployResponse k8sCanaryDeployResponse =
        (K8sCanaryDeployResponse) k8sTaskExecutionResponse.getK8sNGTaskResponse();

    K8sCanaryOutcome k8sCanaryOutcome = K8sCanaryOutcome.builder()
                                            .releaseName(k8sStepHelper.getReleaseName(infrastructure))
                                            .releaseNumber(k8sCanaryDeployResponse.getReleaseNumber())
                                            .targetInstances(k8sCanaryDeployResponse.getCurrentInstances())
                                            .canaryWorkload(k8sCanaryDeployResponse.getCanaryWorkload())
                                            .build();

    return responseBuilder.status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(OutcomeExpressionConstants.K8S_CANARY_OUTCOME)
                         .outcome(k8sCanaryOutcome)
                         .group(StepOutcomeGroup.STAGE.name())
                         .build())
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(OutcomeExpressionConstants.OUTPUT)
                         .outcome(k8sCanaryOutcome)
                         .group(StepOutcomeGroup.STAGE.name())
                         .build())
        .build();
  }

  @Override
  public Class<K8sCanaryStepParameters> getStepParametersClass() {
    return K8sCanaryStepParameters.class;
  }

  private void validate(K8sCanaryStepParameters stepParameters) {
    if (stepParameters.getInstanceSelection() == null || stepParameters.getInstanceSelection().getType() == null
        || stepParameters.getInstanceSelection().getSpec() == null) {
      throw new InvalidRequestException("Instance selection is mandatory");
    }

    String valueType = stepParameters.getInstanceSelection().getType().name().toLowerCase();
    if (stepParameters.getInstanceSelection().getSpec().getInstances() == null) {
      throw new InvalidArgumentsException(String.format("Instance selection %s value is mandatory", valueType));
    }

    if (stepParameters.getInstanceSelection().getSpec().getInstances() <= 0) {
      throw new InvalidArgumentsException(
          String.format("Instance selection %s value cannot be less than 1", valueType));
    }
  }
}
