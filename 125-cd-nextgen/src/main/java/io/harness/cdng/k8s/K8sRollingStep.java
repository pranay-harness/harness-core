package io.harness.cdng.k8s;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sRollingDeployRequest;
import io.harness.delegate.task.k8s.K8sRollingDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
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

public class K8sRollingStep implements TaskChainExecutable<K8sRollingStepParameters>, K8sStepExecutor {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_ROLLING.getYamlType()).build();
  private final String K8S_ROLLING_DEPLOY_COMMAND_NAME = "Rolling Deployment";

  @Inject private K8sStepHelper k8sStepHelper;

  @Override
  public Class<K8sRollingStepParameters> getStepParametersClass() {
    return K8sRollingStepParameters.class;
  }

  @Override
  public TaskChainResponse startChainLink(
      Ambiance ambiance, K8sRollingStepParameters k8sRollingStepParameters, StepInputPackage inputPackage) {
    return k8sStepHelper.startChainLink(this, ambiance, k8sRollingStepParameters);
  }

  public TaskChainResponse executeK8sTask(ManifestOutcome k8sManifestOutcome, Ambiance ambiance,
      K8sStepParameters stepParameters, List<String> valuesFileContents, InfrastructureOutcome infrastructure) {
    String releaseName = k8sStepHelper.getReleaseName(infrastructure);
    boolean skipDryRun =
        !ParameterField.isNull(stepParameters.getSkipDryRun()) && stepParameters.getSkipDryRun().getValue();

    final String accountId = AmbianceHelper.getAccountId(ambiance);
    K8sRollingDeployRequest k8sRollingDeployRequest =
        K8sRollingDeployRequest.builder()
            .skipDryRun(skipDryRun)
            .inCanaryWorkflow(false)
            .releaseName(releaseName)
            .commandName(K8S_ROLLING_DEPLOY_COMMAND_NAME)
            .taskType(K8sTaskType.DEPLOYMENT_ROLLING)
            .localOverrideFeatureFlag(false)
            .timeoutIntervalInMin(K8sStepHelper.getTimeout(stepParameters))
            .valuesYamlList(k8sStepHelper.renderValues(ambiance, valuesFileContents))
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .manifestDelegateConfig(k8sStepHelper.getManifestDelegateConfig(k8sManifestOutcome, ambiance))
            .accountId(accountId)
            .build();

    return k8sStepHelper.queueK8sTask(stepParameters, k8sRollingDeployRequest, ambiance, infrastructure);
  }

  @Override
  public TaskChainResponse executeNextLink(Ambiance ambiance, K8sRollingStepParameters k8sRollingStepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    return k8sStepHelper.executeNextLink(this, ambiance, k8sRollingStepParameters, passThroughData, responseDataMap);
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, K8sRollingStepParameters k8sRollingStepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    if (passThroughData instanceof GitFetchResponsePassThroughData) {
      return k8sStepHelper.handleGitTaskFailure((GitFetchResponsePassThroughData) passThroughData);
    }

    ResponseData responseData = responseDataMap.values().iterator().next();
    if (responseData instanceof ErrorNotifyResponseData) {
      return K8sStepHelper
          .getDelegateErrorFailureResponseBuilder(k8sRollingStepParameters, (ErrorNotifyResponseData) responseData)
          .build();
    }

    K8sDeployResponse k8sTaskExecutionResponse = (K8sDeployResponse) responseData;
    StepResponseBuilder stepResponseBuilder =
        StepResponse.builder().unitProgressList(k8sTaskExecutionResponse.getCommandUnitsProgress().getUnitProgresses());

    if (k8sTaskExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return K8sStepHelper
          .getFailureResponseBuilder(k8sRollingStepParameters, k8sTaskExecutionResponse, stepResponseBuilder)
          .build();
    }

    InfrastructureOutcome infrastructure = (InfrastructureOutcome) passThroughData;
    K8sRollingDeployResponse k8sTaskResponse =
        (K8sRollingDeployResponse) k8sTaskExecutionResponse.getK8sNGTaskResponse();

    K8sRollingOutcome k8sRollingOutcome = K8sRollingOutcome.builder()
                                              .releaseName(k8sStepHelper.getReleaseName(infrastructure))
                                              .releaseNumber(k8sTaskResponse.getReleaseNumber())
                                              .build();

    return stepResponseBuilder.status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(OutcomeExpressionConstants.K8S_ROLL_OUT)
                         .outcome(k8sRollingOutcome)
                         .group(StepOutcomeGroup.STAGE.name())
                         .build())
        .build();
  }
}
