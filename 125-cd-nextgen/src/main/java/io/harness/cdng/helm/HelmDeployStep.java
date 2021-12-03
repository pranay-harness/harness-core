package io.harness.cdng.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.helm.NativeHelmDeployOutcome.NativeHelmDeployOutcomeBuilder;
import io.harness.cdng.helm.beans.NativeHelmExecutionPassThroughData;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.HelmValuesFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.yaml.HelmChartManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTimeConversionHelper;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.task.helm.HelmCmdExecResponseNG;
import io.harness.delegate.task.helm.HelmInstallCmdResponseNG;
import io.harness.delegate.task.helm.HelmInstallCommandRequestNG;
import io.harness.delegate.task.helm.HelmReleaseHistoryCmdResponseNG;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import software.wings.helpers.ext.helm.response.ReleaseInfo;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Slf4j
public class HelmDeployStep extends TaskChainExecutableWithRollbackAndRbac implements NativeHelmStepExecutor {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.HELM_DEPLOY.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  public static final String HELM_COMMAND_NAME = "Helm Deploy";

  @Inject NativeHelmStepHelper nativeHelmStepHelper;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;
  NativeHelmDeployOutcomeBuilder nativeHelmDeployOutcomeBuilder = NativeHelmDeployOutcome.builder();
  NativeHelmDeployOutcome nativeHelmDeployOutcome;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    // Noop
  }

  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepElementParameters stepElementParameters, StepInputPackage inputPackage) {
    return nativeHelmStepHelper.startChainLink(this, ambiance, stepElementParameters);
  }

  @Override
  public TaskChainResponse executeNextLinkWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
      throws Exception {
    ResponseData responseData = responseSupplier.get();
    if (responseData instanceof HelmCmdExecResponseNG) {
      HelmReleaseHistoryCmdResponseNG helmReleaseHistoryCmdResponseNG =
          (HelmReleaseHistoryCmdResponseNG) ((HelmCmdExecResponseNG) responseData).getHelmCommandResponse();
      List<ReleaseInfo> releaseInfoList = helmReleaseHistoryCmdResponseNG.getReleaseInfoList();
      int prevVersion = isEmpty(releaseInfoList)
          ? 0
          : Integer.parseInt(releaseInfoList.get(releaseInfoList.size() - 1).getRevision());
      nativeHelmDeployOutcomeBuilder.prevReleaseVersion(prevVersion);
      nativeHelmDeployOutcomeBuilder.newReleaseVersion(prevVersion + 1);
    }
    return nativeHelmStepHelper.executeNextLink(this, ambiance, stepParameters, passThroughData, responseSupplier);
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    if (passThroughData instanceof GitFetchResponsePassThroughData) {
      return nativeHelmStepHelper.handleGitTaskFailure((GitFetchResponsePassThroughData) passThroughData);
    }

    if (passThroughData instanceof HelmValuesFetchResponsePassThroughData) {
      return nativeHelmStepHelper.handleHelmValuesFetchFailure(
          (HelmValuesFetchResponsePassThroughData) passThroughData);
    }

    if (passThroughData instanceof StepExceptionPassThroughData) {
      return nativeHelmStepHelper.handleStepExceptionFailure((StepExceptionPassThroughData) passThroughData);
    }

    log.info("Finalizing execution with passThroughData: " + passThroughData.getClass().getName());

    NativeHelmExecutionPassThroughData nativeHelmExecutionPassThroughData =
        (NativeHelmExecutionPassThroughData) passThroughData;

    HelmCmdExecResponseNG helmCmdExecResponseNG;
    try {
      helmCmdExecResponseNG = (HelmCmdExecResponseNG) responseDataSupplier.get();
    } catch (Exception e) {
      log.error("Error while processing Helm Task response: {}", e.getMessage(), e);
      return nativeHelmStepHelper.handleTaskException(ambiance, nativeHelmExecutionPassThroughData, e);
    }

    if (helmCmdExecResponseNG.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      executionSweepingOutputService.consume(ambiance, OutcomeExpressionConstants.HELM_DEPLOY_OUTCOME,
          nativeHelmDeployOutcomeBuilder.build(), StepOutcomeGroup.STEP.name());
      return StepResponse.builder()
          .status(Status.FAILED)
          .failureInfo(FailureInfo.newBuilder().setErrorMessage(helmCmdExecResponseNG.getErrorMessage()).build())
          .unitProgressList(helmCmdExecResponseNG.getCommandUnitsProgress().getUnitProgresses())
          .build();
    }

    HelmInstallCmdResponseNG helmInstallCmdResponseNG =
        (HelmInstallCmdResponseNG) helmCmdExecResponseNG.getHelmCommandResponse();
    nativeHelmDeployOutcomeBuilder.containerInfoList(helmInstallCmdResponseNG.getContainerInfoList());
    nativeHelmDeployOutcome = nativeHelmDeployOutcomeBuilder.build();
    executionSweepingOutputService.consume(ambiance, OutcomeExpressionConstants.HELM_DEPLOY_OUTCOME,
        nativeHelmDeployOutcome, StepOutcomeGroup.STEP.name());

    return StepResponse.builder()
        .unitProgressList(helmCmdExecResponseNG.getCommandUnitsProgress().getUnitProgresses())
        .status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(OutcomeExpressionConstants.OUTPUT)
                         .outcome(nativeHelmDeployOutcome)
                         .build())
        .build();
  }

  @Override
  public TaskChainResponse executeHelmTask(ManifestOutcome manifestOutcome, Ambiance ambiance,
      StepElementParameters stepParameters, List<String> valuesFileContents,
      NativeHelmExecutionPassThroughData executionPassThroughData, boolean shouldOpenFetchFilesLogStream,
      UnitProgressData unitProgressData) {
    InfrastructureOutcome infrastructure = executionPassThroughData.getInfrastructure();
    String releaseName = nativeHelmStepHelper.getReleaseName(ambiance, infrastructure);
    List<String> manifestFilesContents =
        nativeHelmStepHelper.renderValues(manifestOutcome, ambiance, valuesFileContents);
    HelmChartManifestOutcome helmChartManifestOutcome = (HelmChartManifestOutcome) manifestOutcome;

    HelmInstallCommandRequestNG helmCommandRequest =
        HelmInstallCommandRequestNG.builder()
            .accountId(AmbianceUtils.getAccountId(ambiance))
            .commandName(HELM_COMMAND_NAME)
            .valuesYamlList(manifestFilesContents)
            .k8sInfraDelegateConfig(nativeHelmStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .manifestDelegateConfig(nativeHelmStepHelper.getManifestDelegateConfig(manifestOutcome, ambiance))
            .commandUnitsProgress(UnitProgressDataMapper.toCommandUnitsProgress(unitProgressData))
            .releaseName(releaseName)
            .helmVersion(helmChartManifestOutcome.getHelmVersion())
            .namespace(nativeHelmStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance).getNamespace())
            .shouldOpenFetchFilesLogStream(true)
            .build();

    if (stepParameters.getTimeout() != null) {
      helmCommandRequest.setTimeoutInMillis(
          NGTimeConversionHelper.convertTimeStringToMilliseconds(stepParameters.getTimeout().getValue()));
    }

    nativeHelmDeployOutcomeBuilder.releaseName(releaseName);

    return nativeHelmStepHelper.queueNativeHelmTask(
        stepParameters, helmCommandRequest, ambiance, executionPassThroughData);
  }
}
