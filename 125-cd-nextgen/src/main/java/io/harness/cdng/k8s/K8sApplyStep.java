package io.harness.cdng.k8s;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.delegate.task.k8s.K8sApplyRequest;
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
import io.harness.pms.sdk.core.steps.io.RollbackOutcome;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;

public class K8sApplyStep implements TaskChainExecutable<K8sApplyStepParameters>, K8sStepExecutor {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_APPLY.getYamlType()).build();
  private final String K8S_APPLY_COMMAND_NAME = "K8s Apply";

  @Inject private K8sStepHelper k8sStepHelper;

  @Override
  public Class<K8sApplyStepParameters> getStepParametersClass() {
    return K8sApplyStepParameters.class;
  }

  @Override
  public TaskChainResponse startChainLink(
      Ambiance ambiance, K8sApplyStepParameters k8sApplyStepParameters, StepInputPackage inputPackage) {
    return k8sStepHelper.startChainLink(this, ambiance, k8sApplyStepParameters);
  }

  @Override
  public TaskChainResponse executeNextLink(Ambiance ambiance, K8sApplyStepParameters k8sApplyStepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    return k8sStepHelper.executeNextLink(this, ambiance, k8sApplyStepParameters, passThroughData, responseDataMap);
  }

  public TaskChainResponse executeK8sTask(K8sManifestOutcome k8sManifestOutcome, Ambiance ambiance,
      K8sStepParameters stepParameters, List<String> valuesFileContents, InfrastructureOutcome infrastructure) {
    StoreConfig storeConfig = k8sManifestOutcome.getStore();
    String releaseName = k8sStepHelper.getReleaseName(infrastructure);
    K8sApplyStepParameters k8sApplyStepParameters = (K8sApplyStepParameters) stepParameters;
    boolean skipDryRun =
        !ParameterField.isNull(k8sApplyStepParameters.getSkipDryRun()) && stepParameters.getSkipDryRun().getValue();
    boolean skipSteadyStateCheck = !ParameterField.isNull(k8sApplyStepParameters.getSkipSteadyStateCheck())
        && k8sApplyStepParameters.getSkipSteadyStateCheck().getValue();

    final String accountId = AmbianceHelper.getAccountId(ambiance);
    K8sApplyRequest k8sApplyRequest =
        K8sApplyRequest.builder()
            .skipDryRun(skipDryRun)
            .releaseName(releaseName)
            .commandName(K8S_APPLY_COMMAND_NAME)
            .taskType(K8sTaskType.APPLY)
            .timeoutIntervalInMin(K8sStepHelper.getTimeout(stepParameters))
            .valuesYamlList(k8sStepHelper.renderValues(ambiance, valuesFileContents))
            .k8sInfraDelegateConfig(k8sStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .manifestDelegateConfig(k8sStepHelper.getManifestDelegateConfig(storeConfig, ambiance))
            .accountId(accountId)
            .deprecateFabric8Enabled(true)
            .filePaths(k8sApplyStepParameters.getFilePaths().getValue())
            .skipSteadyStateCheck(skipSteadyStateCheck)
            .build();

    return k8sStepHelper.queueK8sTask(stepParameters, k8sApplyRequest, ambiance, infrastructure);
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, K8sApplyStepParameters k8sApplyStepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    K8sDeployResponse k8sTaskExecutionResponse = (K8sDeployResponse) responseDataMap.values().iterator().next();

    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    stepResponseBuilder.unitProgressList(k8sTaskExecutionResponse.getCommandUnitsProgress().getUnitProgresses());

    if (k8sTaskExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      stepResponseBuilder.status(Status.FAILED)
          .failureInfo(FailureInfo.newBuilder()
                           .setErrorMessage(K8sStepHelper.getErrorMessage(k8sTaskExecutionResponse))
                           .build());
      if (k8sApplyStepParameters.getRollbackInfo() != null) {
        stepResponseBuilder.stepOutcome(
            StepResponse.StepOutcome.builder()
                .name("RollbackOutcome")
                .outcome(RollbackOutcome.builder().rollbackInfo(k8sApplyStepParameters.getRollbackInfo()).build())
                .build());
      }
      return stepResponseBuilder.build();
    }

    return stepResponseBuilder.status(Status.SUCCEEDED).build();
  }
}