package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.HelmValuesFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.task.k8s.K8sCanaryDeployRequest;
import io.harness.delegate.task.k8s.K8sCanaryDeployResponse;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;

@OwnedBy(CDP)
public class K8sCanaryStep extends TaskChainExecutableWithRollback implements K8sStepExecutor {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_CANARY.getYamlType()).build();
  private final String K8S_CANARY_DEPLOY_COMMAND_NAME = "Canary Deploy";

  @Inject private K8sStepHelper k8sStepHelper;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;

  @Override
  public TaskChainResponse startChainLink(
      Ambiance ambiance, StepElementParameters stepElementParameters, StepInputPackage inputPackage) {
    K8sCanaryStepParameters k8sCanaryStepParameters = (K8sCanaryStepParameters) stepElementParameters.getSpec();
    validate(k8sCanaryStepParameters);
    return k8sStepHelper.startChainLink(this, ambiance, stepElementParameters);
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
    final String releaseName = k8sStepHelper.getReleaseName(infrastructure);
    final K8sCanaryStepParameters canaryStepParameters = (K8sCanaryStepParameters) stepElementParameters.getSpec();
    final Integer instancesValue = canaryStepParameters.getInstanceSelection().getSpec().getInstances();
    final String accountId = AmbianceHelper.getAccountId(ambiance);
    final boolean skipDryRun =
        !ParameterField.isNull(canaryStepParameters.getSkipDryRun()) && canaryStepParameters.getSkipDryRun().getValue();
    List<String> manifestFilesContents = k8sStepHelper.renderValues(k8sManifestOutcome, ambiance, valuesFileContents);
    boolean isOpenshiftTemplate = ManifestType.OpenshiftTemplate.equals(k8sManifestOutcome.getType());

    K8sCanaryDeployRequest k8sCanaryDeployRequest =
        K8sCanaryDeployRequest.builder()
            .skipDryRun(skipDryRun)
            .releaseName(releaseName)
            .commandName(K8S_CANARY_DEPLOY_COMMAND_NAME)
            .taskType(K8sTaskType.CANARY_DEPLOY)
            .instanceUnitType(canaryStepParameters.getInstanceSelection().getType().getInstanceUnitType())
            .instances(instancesValue)
            .timeoutIntervalInMin(K8sStepHelper.getTimeoutInMin(stepElementParameters))
            .valuesYamlList(!isOpenshiftTemplate ? manifestFilesContents : Collections.emptyList())
            .openshiftParamList(isOpenshiftTemplate ? manifestFilesContents : Collections.emptyList())
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .manifestDelegateConfig(k8sStepHelper.getManifestDelegateConfig(k8sManifestOutcome, ambiance))
            .accountId(accountId)
            .skipResourceVersioning(k8sStepHelper.getSkipResourceVersioning(k8sManifestOutcome))
            .shouldOpenFetchFilesLogStream(shouldOpenFetchFilesLogStream)
            .build();

    return k8sStepHelper.queueK8sTask(stepElementParameters, k8sCanaryDeployRequest, ambiance, infrastructure);
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
    StepResponseBuilder responseBuilder =
        StepResponse.builder().unitProgressList(k8sTaskExecutionResponse.getCommandUnitsProgress().getUnitProgresses());
    InfrastructureOutcome infrastructure = (InfrastructureOutcome) passThroughData;
    K8sCanaryDeployResponse k8sCanaryDeployResponse =
        (K8sCanaryDeployResponse) k8sTaskExecutionResponse.getK8sNGTaskResponse();

    K8sCanaryOutcome k8sCanaryOutcome = K8sCanaryOutcome.builder()
                                            .releaseName(k8sStepHelper.getReleaseName(infrastructure))
                                            .releaseNumber(k8sCanaryDeployResponse.getReleaseNumber())
                                            .targetInstances(k8sCanaryDeployResponse.getCurrentInstances())
                                            .canaryWorkload(k8sCanaryDeployResponse.getCanaryWorkload())
                                            .canaryWorkloadDeployed(k8sCanaryDeployResponse.isCanaryWorkloadDeployed())
                                            .build();

    executionSweepingOutputService.consume(
        ambiance, OutcomeExpressionConstants.K8S_CANARY_OUTCOME, k8sCanaryOutcome, StepOutcomeGroup.STAGE.name());
    if (k8sTaskExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return K8sStepHelper.getFailureResponseBuilder(k8sTaskExecutionResponse, responseBuilder).build();
    }
    return responseBuilder.status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(OutcomeExpressionConstants.OUTPUT)
                         .outcome(k8sCanaryOutcome)
                         .build())
        .build();
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
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
