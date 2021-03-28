package io.harness.cdng.k8s;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.k8s.K8sBGDeployRequest;
import io.harness.delegate.task.k8s.K8sBGDeployResponse;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class K8sBlueGreenStep implements TaskChainExecutable<K8sBlueGreenStepParameters>, K8sStepExecutor {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_BLUE_GREEN.getYamlType()).build();
  public static final String K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME = "Blue/Green Deploy";

  @Inject private K8sStepHelper k8sStepHelper;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;

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

  public TaskChainResponse executeK8sTask(ManifestOutcome k8sManifestOutcome, Ambiance ambiance,
      K8sStepParameters stepParameters, List<String> valuesFileContents, InfrastructureOutcome infrastructure) {
    String releaseName = k8sStepHelper.getReleaseName(infrastructure);
    boolean skipDryRun =
        !ParameterField.isNull(stepParameters.getSkipDryRun()) && stepParameters.getSkipDryRun().getValue();
    List<String> manifestFilesContents = k8sStepHelper.renderValues(k8sManifestOutcome, ambiance, valuesFileContents);
    boolean isOpenshiftTemplate = ManifestType.OpenshiftTemplate.equals(k8sManifestOutcome.getType());

    final String accountId = AmbianceHelper.getAccountId(ambiance);
    K8sBGDeployRequest k8sBGDeployRequest =
        K8sBGDeployRequest.builder()
            .skipDryRun(skipDryRun)
            .releaseName(releaseName)
            .commandName(K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME)
            .taskType(K8sTaskType.BLUE_GREEN_DEPLOY)
            .timeoutIntervalInMin(K8sStepHelper.getTimeout(stepParameters))
            .valuesYamlList(!isOpenshiftTemplate ? manifestFilesContents : Collections.emptyList())
            .openshiftParamList(isOpenshiftTemplate ? manifestFilesContents : Collections.emptyList())
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .manifestDelegateConfig(k8sStepHelper.getManifestDelegateConfig(k8sManifestOutcome, ambiance))
            .accountId(accountId)
            .skipResourceVersioning(k8sStepHelper.getSkipResourceVersioning(k8sManifestOutcome))
            .build();

    return k8sStepHelper.queueK8sTask(stepParameters, k8sBGDeployRequest, ambiance, infrastructure);
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, K8sBlueGreenStepParameters k8sBlueGreenStepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    if (passThroughData instanceof GitFetchResponsePassThroughData) {
      return k8sStepHelper.handleGitTaskFailure((GitFetchResponsePassThroughData) passThroughData);
    }

    ResponseData responseData = responseDataMap.values().iterator().next();
    if (responseData instanceof ErrorNotifyResponseData) {
      return K8sStepHelper
          .getDelegateErrorFailureResponseBuilder(k8sBlueGreenStepParameters, (ErrorNotifyResponseData) responseData)
          .build();
    }

    K8sDeployResponse k8sTaskExecutionResponse = (K8sDeployResponse) responseData;
    StepResponseBuilder responseBuilder =
        StepResponse.builder().unitProgressList(k8sTaskExecutionResponse.getCommandUnitsProgress().getUnitProgresses());

    if (k8sTaskExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return K8sStepHelper
          .getFailureResponseBuilder(k8sBlueGreenStepParameters, k8sTaskExecutionResponse, responseBuilder)
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
    executionSweepingOutputService.consume(ambiance, OutcomeExpressionConstants.K8S_BLUE_GREEN_OUTCOME,
        k8sBlueGreenOutcome, StepOutcomeGroup.STAGE.name());

    return responseBuilder.status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(OutcomeExpressionConstants.OUTPUT)
                         .outcome(k8sBlueGreenOutcome)
                         .build())
        .build();
  }
}
