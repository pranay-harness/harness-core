package io.harness.cdng.k8s;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTimeConversionHelper;
import io.harness.delegate.task.k8s.K8sBGDeployRequest;
import io.harness.delegate.task.k8s.K8sBGDeployResponse;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.TaskChainExecutable;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;

public class K8sBlueGreenStep implements TaskChainExecutable<K8sBlueGreenStepParameters>, K8sStepExecutor {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_BLUE_GREEN.getName()).build();
  public static final String K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME = "Blue/Green Deployment";

  @Inject private K8sStepHelper k8sStepHelper;

  @Override
  public Class<K8sBlueGreenStepParameters> getStepParametersClass() {
    return K8sBlueGreenStepParameters.class;
  }

  @Override
  public TaskChainResponse startChainLink(
      Ambiance ambiance, K8sBlueGreenStepParameters k8sBlueGreenStepParameters, StepInputPackage inputPackage) {
    return k8sStepHelper.startChainLink(this, ambiance, k8sBlueGreenStepParameters);
  }

  @Override
  public TaskChainResponse executeNextLink(Ambiance ambiance, K8sBlueGreenStepParameters k8sBlueGreenStepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    return k8sStepHelper.executeNextLink(this, ambiance, k8sBlueGreenStepParameters, passThroughData, responseDataMap);
  }

  public TaskChainResponse executeK8sTask(K8sManifestOutcome k8sManifestOutcome, Ambiance ambiance,
      K8sStepParameters stepParameters, List<String> valuesFileContents, InfrastructureOutcome infrastructure) {
    StoreConfig storeConfig = k8sManifestOutcome.getStore().getStoreConfig();
    String releaseName = k8sStepHelper.getReleaseName(infrastructure);

    final String accountId = AmbianceHelper.getAccountId(ambiance);
    K8sBGDeployRequest k8sBGDeployRequest =
        K8sBGDeployRequest.builder()
            .skipDryRun(stepParameters.getSkipDryRun().getValue())
            .releaseName(releaseName)
            .commandName(K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME)
            .taskType(K8sTaskType.BLUE_GREEN_DEPLOY)
            .timeoutIntervalInMin(
                NGTimeConversionHelper.convertTimeStringToMinutes(stepParameters.getTimeout().getValue()))
            .valuesYamlList(k8sStepHelper.renderValues(ambiance, valuesFileContents))
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .manifestDelegateConfig(k8sStepHelper.getManifestDelegateConfig(storeConfig, ambiance))
            .accountId(accountId)
            .build();

    return k8sStepHelper.queueK8sTask(stepParameters, k8sBGDeployRequest, ambiance, infrastructure);
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, K8sBlueGreenStepParameters k8sBlueGreenStepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    K8sDeployResponse k8sTaskExecutionResponse = (K8sDeployResponse) responseDataMap.values().iterator().next();

    if (k8sTaskExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return StepResponse.builder()
          .status(Status.FAILED)
          .failureInfo(FailureInfo.newBuilder().setErrorMessage(k8sTaskExecutionResponse.getErrorMessage()).build())
          .build();
    }

    InfrastructureOutcome infrastructure = (InfrastructureOutcome) passThroughData;
    K8sBGDeployResponse k8sBGDeployResponse = (K8sBGDeployResponse) k8sTaskExecutionResponse.getK8sNGTaskResponse();

    K8sBlueGreenOutcome k8sBlueGreenOutcome = K8sBlueGreenOutcome.builder()
                                                  .releaseName(k8sStepHelper.getReleaseName(infrastructure))
                                                  .releaseNumber(k8sBGDeployResponse.getReleaseNumber())
                                                  .primaryServiceName(k8sBGDeployResponse.getPrimaryServiceName())
                                                  .stageServiceName(k8sBGDeployResponse.getStageServiceName())
                                                  .stageColor(k8sBGDeployResponse.getStageColor())
                                                  .primaryColor(k8sBGDeployResponse.getPrimaryColor())
                                                  .build();

    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(OutcomeExpressionConstants.K8S_BLUE_GREEN_OUTCOME)
                         .outcome(k8sBlueGreenOutcome)
                         .build())
        .build();
  }
}
