package io.harness.cdng.k8s;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngpipeline.common.ParameterFieldHelper.getParameterFieldValue;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.k8s.K8sApplyBaseStepInfo.K8sApplyBaseStepInfoKeys;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.HelmValuesFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.delegate.task.k8s.K8sApplyRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(HarnessTeam.CDP)
public class K8sApplyStep extends TaskChainExecutableWithRollback implements K8sStepExecutor {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_APPLY.getYamlType()).build();
  private final String K8S_APPLY_COMMAND_NAME = "K8s Apply";

  @Inject private K8sStepHelper k8sStepHelper;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public TaskChainResponse startChainLink(
      Ambiance ambiance, StepElementParameters stepElementParameters, StepInputPackage inputPackage) {
    K8sApplyStepParameters k8sApplyStepParameters = (K8sApplyStepParameters) stepElementParameters.getSpec();
    validateFilePaths(k8sApplyStepParameters);
    return k8sStepHelper.startChainLink(this, ambiance, stepElementParameters);
  }

  private void validateFilePaths(K8sApplyStepParameters k8sApplyStepParameters) {
    if (ParameterField.isNull(k8sApplyStepParameters.getFilePaths())) {
      throw new InvalidRequestException("File/Folder path must be present");
    }

    if (isEmpty(getParameterFieldValue(k8sApplyStepParameters.getFilePaths()))) {
      throw new InvalidRequestException("File/Folder path must be present");
    }

    List<String> filePaths = getParameterFieldValue(k8sApplyStepParameters.getFilePaths());
    for (String filePath : filePaths) {
      if (isEmpty(filePath)) {
        throw new InvalidRequestException("File/Folder path must be present");
      }
    }
  }

  @Override
  public TaskChainResponse executeNextLink(Ambiance ambiance, StepElementParameters stepElementParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
      throws Exception {
    return k8sStepHelper.executeNextLink(this, ambiance, stepElementParameters, passThroughData, responseSupplier);
  }

  @Override
  public TaskChainResponse executeK8sTask(ManifestOutcome k8sManifestOutcome, Ambiance ambiance,
      StepElementParameters stepElementParameters, List<String> valuesFileContents,
      InfrastructureOutcome infrastructure, boolean shouldOpenFetchFilesLogStream) {
    String releaseName = k8sStepHelper.getReleaseName(infrastructure);
    K8sApplyStepParameters k8sApplyStepParameters = (K8sApplyStepParameters) stepElementParameters.getSpec();
    boolean skipDryRun = K8sStepHelper.getParameterFieldBooleanValue(
        k8sApplyStepParameters.getSkipDryRun(), K8sApplyBaseStepInfoKeys.skipDryRun, stepElementParameters);
    boolean skipSteadyStateCheck =
        K8sStepHelper.getParameterFieldBooleanValue(k8sApplyStepParameters.getSkipSteadyStateCheck(),
            K8sApplyBaseStepInfoKeys.skipSteadyStateCheck, stepElementParameters);

    final String accountId = AmbianceHelper.getAccountId(ambiance);
    K8sApplyRequest k8sApplyRequest =
        K8sApplyRequest.builder()
            .skipDryRun(skipDryRun)
            .releaseName(releaseName)
            .commandName(K8S_APPLY_COMMAND_NAME)
            .taskType(K8sTaskType.APPLY)
            .timeoutIntervalInMin(K8sStepHelper.getTimeoutInMin(stepElementParameters))
            .valuesYamlList(k8sStepHelper.renderValues(k8sManifestOutcome, ambiance, valuesFileContents))
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .manifestDelegateConfig(k8sStepHelper.getManifestDelegateConfig(k8sManifestOutcome, ambiance))
            .accountId(accountId)
            .deprecateFabric8Enabled(true)
            .filePaths(k8sApplyStepParameters.getFilePaths().getValue())
            .skipSteadyStateCheck(skipSteadyStateCheck)
            .shouldOpenFetchFilesLogStream(shouldOpenFetchFilesLogStream)
            .build();
    return k8sStepHelper.queueK8sTask(stepElementParameters, k8sApplyRequest, ambiance, infrastructure);
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, StepElementParameters stepElementParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    if (passThroughData instanceof GitFetchResponsePassThroughData) {
      return k8sStepHelper.handleGitTaskFailure((GitFetchResponsePassThroughData) passThroughData);
    }

    if (passThroughData instanceof HelmValuesFetchResponsePassThroughData) {
      return k8sStepHelper.handleHelmValuesFetchFailure((HelmValuesFetchResponsePassThroughData) passThroughData);
    }

    if (passThroughData instanceof StepExceptionPassThroughData) {
      return k8sStepHelper.handleStepExceptionFailure((StepExceptionPassThroughData) passThroughData);
    }

    K8sDeployResponse k8sTaskExecutionResponse = (K8sDeployResponse) responseDataSupplier.get();
    StepResponseBuilder stepResponseBuilder =
        StepResponse.builder().unitProgressList(k8sTaskExecutionResponse.getCommandUnitsProgress().getUnitProgresses());

    if (k8sTaskExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return K8sStepHelper.getFailureResponseBuilder(k8sTaskExecutionResponse, stepResponseBuilder).build();
    }
    return stepResponseBuilder.status(Status.SUCCEEDED).build();
  }
}