package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.ParameterFieldHelper;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.terraform.TFTaskType;
import io.harness.delegate.task.terraform.TerraformCommand;
import io.harness.delegate.task.terraform.TerraformCommandUnit;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters.TerraformTaskNGParametersBuilder;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.UnitProgress;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.provision.TerraformConstants;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class TerraformApplyStep extends TaskExecutableWithRollback<TerraformTaskNGResponse> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.TERRAFORM_APPLY.getYamlType()).build();

  @Inject private KryoSerializer kryoSerializer;
  @Inject private TerraformStepHelper helper;
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, StepElementParameters stepElementParameters, StepInputPackage inputPackage) {
    TerraformApplyStepParameters stepParameters = (TerraformApplyStepParameters) stepElementParameters.getSpec();
    helper.validateApplyStepParamsInline(stepParameters);
    TerraformStepConfigurationType configurationType = stepParameters.getConfiguration().getType();
    switch (configurationType) {
      case INLINE:
        return obtainInlineTask(ambiance, stepParameters, stepElementParameters);
      case INHERIT_FROM_PLAN:
        return obtainInheritedTask(ambiance, stepParameters, stepElementParameters);
      default:
        throw new InvalidRequestException(
            String.format("Unknown configuration Type: [%s]", configurationType.getDisplayName()));
    }
  }

  private TaskRequest obtainInlineTask(
      Ambiance ambiance, TerraformApplyStepParameters stepParameters, StepElementParameters stepElementParameters) {
    helper.validateApplyStepConfigFilesInline(stepParameters);
    TerraformStepConfigurationParameters configuration = stepParameters.getConfiguration();
    TerraformExecutionDataParameters spec = configuration.getSpec();
    TerraformTaskNGParametersBuilder builder = TerraformTaskNGParameters.builder();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    builder.accountId(accountId);
    String provisionerIdentifier =
        ParameterFieldHelper.getParameterFieldValue(stepParameters.getProvisionerIdentifier());
    String entityId = helper.generateFullIdentifier(provisionerIdentifier, ambiance);
    builder.currentStateFileId(helper.getLatestFileId(entityId))
        .taskType(TFTaskType.APPLY)
        .terraformCommand(TerraformCommand.APPLY)
        .terraformCommandUnit(TerraformCommandUnit.Apply)
        .entityId(entityId)
        .workspace(ParameterFieldHelper.getParameterFieldValue(spec.getWorkspace()))
        .configFile(helper.getGitFetchFilesConfig(
            spec.getConfigFiles().getStore().getSpec(), ambiance, TerraformStepHelper.TF_CONFIG_FILES))
        .varFileInfos(helper.toTerraformVarFileInfo(spec.getVarFiles(), ambiance))
        .backendConfig(helper.getBackendConfig(spec.getBackendConfig()))
        .targets(ParameterFieldHelper.getParameterFieldValue(spec.getTargets()))
        .saveTerraformStateJson(false)
        .environmentVariables(helper.getEnvironmentVariablesMap(spec.getEnvironmentVariables()))
        .timeoutInMillis(
            StepUtils.getTimeoutMillis(stepElementParameters.getTimeout(), TerraformConstants.DEFAULT_TIMEOUT));

    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.TERRAFORM_TASK_NG.name())
            .timeout(StepUtils.getTimeoutMillis(stepElementParameters.getTimeout(), TerraformConstants.DEFAULT_TIMEOUT))
            .parameters(new Object[] {builder.build()})
            .build();

    return StepUtils.prepareTaskRequestWithTaskSelector(ambiance, taskData, kryoSerializer,
        Collections.singletonList(TerraformCommandUnit.Apply.name()), TaskType.TERRAFORM_TASK_NG.getDisplayName(),
        StepUtils.getTaskSelectors(stepParameters.getDelegateSelectors()));
  }

  private TaskRequest obtainInheritedTask(
      Ambiance ambiance, TerraformApplyStepParameters stepParameters, StepElementParameters stepElementParameters) {
    TerraformTaskNGParametersBuilder builder =
        TerraformTaskNGParameters.builder().taskType(TFTaskType.APPLY).terraformCommandUnit(TerraformCommandUnit.Apply);
    String accountId = AmbianceHelper.getAccountId(ambiance);
    builder.accountId(accountId);
    String provisionerIdentifier =
        ParameterFieldHelper.getParameterFieldValue(stepParameters.getProvisionerIdentifier());
    String entityId = helper.generateFullIdentifier(provisionerIdentifier, ambiance);
    builder.entityId(entityId);
    builder.currentStateFileId(helper.getLatestFileId(entityId));
    TerraformInheritOutput inheritOutput = helper.getSavedInheritOutput(provisionerIdentifier, ambiance);
    builder.workspace(inheritOutput.getWorkspace())
        .configFile(helper.getGitFetchFilesConfig(
            inheritOutput.getConfigFiles(), ambiance, TerraformStepHelper.TF_CONFIG_FILES))
        .varFileInfos(helper.prepareTerraformVarFileInfo(inheritOutput.getVarFileConfigs(), ambiance))
        .backendConfig(inheritOutput.getBackendConfig())
        .targets(inheritOutput.getTargets())
        .saveTerraformStateJson(false)
        .encryptionConfig(inheritOutput.getEncryptionConfig())
        .encryptedTfPlan(inheritOutput.getEncryptedTfPlan())
        .planName(inheritOutput.getPlanName())
        .environmentVariables(inheritOutput.getEnvironmentVariables())
        .timeoutInMillis(
            StepUtils.getTimeoutMillis(stepElementParameters.getTimeout(), TerraformConstants.DEFAULT_TIMEOUT));

    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.TERRAFORM_TASK_NG.name())
            .timeout(StepUtils.getTimeoutMillis(stepElementParameters.getTimeout(), TerraformConstants.DEFAULT_TIMEOUT))
            .parameters(new Object[] {builder.build()})
            .build();

    return StepUtils.prepareTaskRequestWithTaskSelector(ambiance, taskData, kryoSerializer,
        Collections.singletonList(TerraformCommandUnit.Apply.name()), TaskType.TERRAFORM_TASK_NG.getDisplayName(),
        StepUtils.getTaskSelectors(stepParameters.getDelegateSelectors()));
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, StepElementParameters stepElementParameters,
      ThrowingSupplier<TerraformTaskNGResponse> responseSupplier) throws Exception {
    TerraformApplyStepParameters stepParameters = (TerraformApplyStepParameters) stepElementParameters.getSpec();
    TerraformStepConfigurationType configurationType = stepParameters.getConfiguration().getType();
    switch (configurationType) {
      case INLINE:
        return handleTaskResultInline(ambiance, stepParameters, responseSupplier);
      case INHERIT_FROM_PLAN:
        return handleTaskResultInherited(ambiance, stepParameters, responseSupplier);
      default:
        throw new InvalidRequestException(
            String.format("Unknown configuration Type: [%s]", configurationType.getDisplayName()));
    }
  }

  private StepResponseBuilder createStepResponseBuilder(ThrowingSupplier<TerraformTaskNGResponse> responseSupplier)
      throws Exception {
    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    TerraformTaskNGResponse terraformTaskNGResponse = responseSupplier.get();
    List<UnitProgress> unitProgresses = terraformTaskNGResponse.getUnitProgressData() == null
        ? Collections.emptyList()
        : terraformTaskNGResponse.getUnitProgressData().getUnitProgresses();
    stepResponseBuilder.unitProgressList(unitProgresses);

    switch (terraformTaskNGResponse.getCommandExecutionStatus()) {
      case SUCCESS:
        stepResponseBuilder.status(Status.SUCCEEDED);
        break;
      case FAILURE:
        stepResponseBuilder.status(Status.FAILED);
        break;
      case RUNNING:
        stepResponseBuilder.status(Status.RUNNING);
        break;
      case QUEUED:
        stepResponseBuilder.status(Status.QUEUED);
        break;
      default:
        throw new InvalidRequestException(
            "Unhandled type CommandExecutionStatus: " + terraformTaskNGResponse.getCommandExecutionStatus().name(),
            WingsException.USER);
    }
    return stepResponseBuilder;
  }

  private void addStepOutcomeToStepResponse(
      StepResponseBuilder stepResponseBuilder, TerraformTaskNGResponse terraformTaskNGResponse) {
    stepResponseBuilder.stepOutcome(
        StepResponse.StepOutcome.builder()
            .name(OutcomeExpressionConstants.OUTPUT)
            .outcome(TerraformApplyOutcome.builder()
                         .outputs(helper.parseTerraformOutputs(terraformTaskNGResponse.getOutputs()))
                         .build())
            .build());
  }

  private StepResponse handleTaskResultInline(Ambiance ambiance, TerraformApplyStepParameters stepParameters,
      ThrowingSupplier<TerraformTaskNGResponse> responseSupplier) throws Exception {
    StepResponseBuilder stepResponseBuilder = createStepResponseBuilder(responseSupplier);
    TerraformTaskNGResponse terraformTaskNGResponse = responseSupplier.get();
    List<UnitProgress> unitProgresses = terraformTaskNGResponse.getUnitProgressData() == null
        ? Collections.emptyList()
        : terraformTaskNGResponse.getUnitProgressData().getUnitProgresses();
    stepResponseBuilder.unitProgressList(unitProgresses);
    if (CommandExecutionStatus.SUCCESS == terraformTaskNGResponse.getCommandExecutionStatus()) {
      helper.saveRollbackDestroyConfigInline(stepParameters, terraformTaskNGResponse, ambiance);
      addStepOutcomeToStepResponse(stepResponseBuilder, terraformTaskNGResponse);
      helper.updateParentEntityIdAndVersion(
          helper.generateFullIdentifier(
              ParameterFieldHelper.getParameterFieldValue(stepParameters.getProvisionerIdentifier()), ambiance),
          terraformTaskNGResponse.getStateFileId());
    }
    return stepResponseBuilder.build();
  }

  private StepResponse handleTaskResultInherited(Ambiance ambiance, TerraformApplyStepParameters stepParameters,
      ThrowingSupplier<TerraformTaskNGResponse> responseSupplier) throws Exception {
    StepResponseBuilder stepResponseBuilder = createStepResponseBuilder(responseSupplier);
    TerraformTaskNGResponse terraformTaskNGResponse = responseSupplier.get();
    List<UnitProgress> unitProgresses = terraformTaskNGResponse.getUnitProgressData() == null
        ? Collections.emptyList()
        : terraformTaskNGResponse.getUnitProgressData().getUnitProgresses();
    stepResponseBuilder.unitProgressList(unitProgresses);
    if (CommandExecutionStatus.SUCCESS == terraformTaskNGResponse.getCommandExecutionStatus()) {
      helper.saveRollbackDestroyConfigInherited(stepParameters, ambiance);
      addStepOutcomeToStepResponse(stepResponseBuilder, terraformTaskNGResponse);
      helper.updateParentEntityIdAndVersion(
          helper.generateFullIdentifier(
              ParameterFieldHelper.getParameterFieldValue(stepParameters.getProvisionerIdentifier()), ambiance),
          terraformTaskNGResponse.getStateFileId());
    }
    return stepResponseBuilder.build();
  }
}
