package io.harness.cdng.k8s;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.k8s.K8sBlueGreenBaseStepInfo.K8sBlueGreenBaseStepInfoKeys;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.HelmValuesFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.K8sExecutionPassThroughData;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.task.k8s.K8sBGDeployRequest;
import io.harness.delegate.task.k8s.K8sBGDeployResponse;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;

@OwnedBy(HarnessTeam.CDP)
public class K8sBlueGreenStep extends TaskChainExecutableWithRollbackAndRbac implements K8sStepExecutor {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.K8S_BLUE_GREEN.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  public static final String K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME = "Blue/Green Deploy";

  @Inject private K8sStepHelper k8sStepHelper;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    // Noop
  }

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepElementParameters stepElementParameters, StepInputPackage inputPackage) {
    return k8sStepHelper.startChainLink(this, ambiance, stepElementParameters);
  }

  @Override
  public TaskChainResponse executeNextLinkWithSecurityContext(Ambiance ambiance,
      StepElementParameters stepElementParameters, StepInputPackage inputPackage, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseSupplier) throws Exception {
    return k8sStepHelper.executeNextLink(this, ambiance, stepElementParameters, passThroughData, responseSupplier);
  }

  @Override
  public TaskChainResponse executeK8sTask(ManifestOutcome k8sManifestOutcome, Ambiance ambiance,
      StepElementParameters stepElementParameters, List<String> valuesFileContents,
      K8sExecutionPassThroughData executionPassThroughData, boolean shouldOpenFetchFilesLogStream) {
    InfrastructureOutcome infrastructure = executionPassThroughData.getInfrastructure();
    String releaseName = k8sStepHelper.getReleaseName(infrastructure);
    K8sBlueGreenStepParameters k8sBlueGreenStepParameters =
        (K8sBlueGreenStepParameters) stepElementParameters.getSpec();
    boolean skipDryRun = K8sStepHelper.getParameterFieldBooleanValue(
        k8sBlueGreenStepParameters.getSkipDryRun(), K8sBlueGreenBaseStepInfoKeys.skipDryRun, stepElementParameters);
    List<String> manifestFilesContents = k8sStepHelper.renderValues(k8sManifestOutcome, ambiance, valuesFileContents);
    boolean isOpenshiftTemplate = ManifestType.OpenshiftTemplate.equals(k8sManifestOutcome.getType());

    final String accountId = AmbianceHelper.getAccountId(ambiance);
    K8sBGDeployRequest k8sBGDeployRequest =
        K8sBGDeployRequest.builder()
            .skipDryRun(skipDryRun)
            .releaseName(releaseName)
            .commandName(K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME)
            .taskType(K8sTaskType.BLUE_GREEN_DEPLOY)
            .timeoutIntervalInMin(K8sStepHelper.getTimeoutInMin(stepElementParameters))
            .valuesYamlList(!isOpenshiftTemplate ? manifestFilesContents : Collections.emptyList())
            .openshiftParamList(isOpenshiftTemplate ? manifestFilesContents : Collections.emptyList())
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .manifestDelegateConfig(k8sStepHelper.getManifestDelegateConfig(k8sManifestOutcome, ambiance))
            .accountId(accountId)
            .skipResourceVersioning(k8sStepHelper.getSkipResourceVersioning(k8sManifestOutcome))
            .shouldOpenFetchFilesLogStream(shouldOpenFetchFilesLogStream)
            .build();

    return k8sStepHelper.queueK8sTask(stepElementParameters, k8sBGDeployRequest, ambiance, executionPassThroughData);
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance,
      StepElementParameters stepElementParameters, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseDataSupplier) {
    if (passThroughData instanceof GitFetchResponsePassThroughData) {
      return k8sStepHelper.handleGitTaskFailure((GitFetchResponsePassThroughData) passThroughData);
    }

    if (passThroughData instanceof HelmValuesFetchResponsePassThroughData) {
      return k8sStepHelper.handleHelmValuesFetchFailure((HelmValuesFetchResponsePassThroughData) passThroughData);
    }

    if (passThroughData instanceof StepExceptionPassThroughData) {
      return k8sStepHelper.handleStepExceptionFailure((StepExceptionPassThroughData) passThroughData);
    }

    K8sExecutionPassThroughData executionPassThroughData = (K8sExecutionPassThroughData) passThroughData;
    try {
      K8sDeployResponse k8sTaskExecutionResponse = (K8sDeployResponse) responseDataSupplier.get();
      StepResponseBuilder responseBuilder = StepResponse.builder().unitProgressList(
          k8sTaskExecutionResponse.getCommandUnitsProgress().getUnitProgresses());

      if (k8sTaskExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
        return K8sStepHelper.getFailureResponseBuilder(k8sTaskExecutionResponse, responseBuilder).build();
      }

      InfrastructureOutcome infrastructure = executionPassThroughData.getInfrastructure();
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
    } catch (Exception e) {
      return k8sStepHelper.handleTaskException(ambiance, executionPassThroughData, e);
    }
  }
}
