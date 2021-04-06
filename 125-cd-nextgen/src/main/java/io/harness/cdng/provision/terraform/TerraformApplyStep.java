package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.terraform.TFTaskType;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters.TerraformTaskNGParametersBuilder;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ff.FeatureFlagService;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.UnitProgress;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.ngpipeline.common.ParameterFieldHelper;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.provision.TerraformConstants;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;

import software.wings.beans.TaskType;
import software.wings.service.intfc.FileService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class TerraformApplyStep implements TaskExecutable<StepElementParameters, TerraformTaskNGResponse> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.TERRAFORM_APPLY.getYamlType()).build();

  @Inject private KryoSerializer kryoSerializer;
  @Inject private TerraformStepHelper helper;
  @Inject private FileService fileService;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, StepElementParameters stepElementParameters, StepInputPackage inputPackage) {
    TerraformApplyStepParameters stepParameters = (TerraformApplyStepParameters) stepElementParameters.getSpec();
    TerraformStepConfigurationType configurationType = stepParameters.getStepConfigurationType();
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
    TerraformTaskNGParametersBuilder builder = TerraformTaskNGParameters.builder();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    builder.accountId(accountId);
    String entityId = helper.generateFullIdentifier(stepParameters.getProvisionerIdentifier(), ambiance);
    builder.currentStateFileId(fileService.getLatestFileId(entityId, FileBucket.TERRAFORM_STATE))
        .taskType(TFTaskType.APPLY)
        .provisionerIdentifier(stepParameters.getProvisionerIdentifier())
        .workspace(ParameterFieldHelper.getParameterFieldValue(stepParameters.getWorkspace()))
        .configFiles(helper.getGitFetchFilesConfig(
            stepParameters.getConfigFilesWrapper().getStoreConfig(), ambiance, TerraformStepHelper.TF_CONFIG_FILES))
        .inlineVarFiles(ParameterFieldHelper.getParameterFieldValue(stepParameters.getInlineVarFiles()));
    if (EmptyPredicate.isNotEmpty(stepParameters.getRemoteVarFiles())) {
      List<GitFetchFilesConfig> varFilesConfig = new ArrayList<>();
      int i = 1;
      for (StoreConfigWrapper varFileWrapper : stepParameters.getRemoteVarFiles()) {
        varFilesConfig.add(helper.getGitFetchFilesConfig(
            varFileWrapper.getStoreConfig(), ambiance, String.format(TerraformStepHelper.TF_VAR_FILES, i)));
        i++;
      }
      builder.remoteVarfiles(varFilesConfig);
    }
    builder.backendConfig(ParameterFieldHelper.getParameterFieldValue(stepParameters.getBackendConfig()))
        .targets(ParameterFieldHelper.getParameterFieldValue(stepParameters.getTargets()))
        .saveTerraformStateJson(featureFlagService.isEnabled(FeatureName.EXPORT_TF_PLAN, accountId))
        .environmentVariables(helper.getEnvironmentVariablesMap(stepParameters.getEnvironmentVariables()));

    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.TERRAFORM_TASK_NG.name())
            .timeout(StepUtils.getTimeoutMillis(stepElementParameters.getTimeout(), TerraformConstants.DEFAULT_TIMEOUT))
            .parameters(new Object[] {builder.build()})
            .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer,
        Collections.singletonList(TerraformConstants.COMMAND_UNIT), TaskType.TERRAFORM_TASK_NG.getDisplayName());
  }

  private TaskRequest obtainInheritedTask(
      Ambiance ambiance, TerraformApplyStepParameters stepParameters, StepElementParameters stepElementParameters) {
    TerraformTaskNGParametersBuilder builder = TerraformTaskNGParameters.builder()
                                                   .taskType(TFTaskType.APPLY)
                                                   .provisionerIdentifier(stepParameters.getProvisionerIdentifier());
    String accountId = AmbianceHelper.getAccountId(ambiance);
    builder.accountId(accountId);
    String entityId = helper.generateFullIdentifier(stepParameters.getProvisionerIdentifier(), ambiance);
    builder.currentStateFileId(fileService.getLatestFileId(entityId, FileBucket.TERRAFORM_STATE));
    TerraformInheritOutput inheritOutput =
        helper.getSavedInheritOutput(stepParameters.getProvisionerIdentifier(), ambiance);
    builder.workspace(inheritOutput.getWorkspace())
        .configFiles(helper.getGitFetchFilesConfig(
            inheritOutput.getConfigFiles(), ambiance, TerraformStepHelper.TF_CONFIG_FILES));
    if (EmptyPredicate.isNotEmpty(inheritOutput.getRemoteVarFiles())) {
      List<GitFetchFilesConfig> varFilesConfig = new ArrayList<>();
      int i = 1;
      for (StoreConfig storeConfig : inheritOutput.getRemoteVarFiles()) {
        varFilesConfig.add(
            helper.getGitFetchFilesConfig(storeConfig, ambiance, String.format(TerraformStepHelper.TF_VAR_FILES, i)));
        i++;
      }
      builder.remoteVarfiles(varFilesConfig);
    }
    builder.inlineVarFiles(inheritOutput.getInlineVarFiles())
        .backendConfig(inheritOutput.getBackendConfig())
        .targets(inheritOutput.getTargets())
        .saveTerraformStateJson(featureFlagService.isEnabled(FeatureName.EXPORT_TF_PLAN, accountId))
        .encryptionConfig(inheritOutput.getEncryptionConfig())
        .encryptedTfPlan(inheritOutput.getEncryptedTfPlan())
        .planName(inheritOutput.getPlanName())
        .environmentVariables(inheritOutput.getEnvironmentVariables());

    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.TERRAFORM_TASK_NG.name())
            .timeout(StepUtils.getTimeoutMillis(stepElementParameters.getTimeout(), TerraformConstants.DEFAULT_TIMEOUT))
            .parameters(new Object[] {builder.build()})
            .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer,
        Collections.singletonList(TerraformConstants.COMMAND_UNIT), TaskType.TERRAFORM_TASK_NG.getDisplayName());
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, StepElementParameters stepElementParameters,
      ThrowingSupplier<TerraformTaskNGResponse> responseSupplier) throws Exception {
    TerraformApplyStepParameters stepParameters = (TerraformApplyStepParameters) stepElementParameters.getSpec();
    TerraformStepConfigurationType configurationType = stepParameters.getStepConfigurationType();
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

  private StepResponse handleTaskResultInline(Ambiance ambiance, TerraformApplyStepParameters stepParameters,
      ThrowingSupplier<TerraformTaskNGResponse> responseSupplier) throws Exception {
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

    if (CommandExecutionStatus.SUCCESS == terraformTaskNGResponse.getCommandExecutionStatus()) {
      helper.saveRollbackDestroyConfigInline(stepParameters, terraformTaskNGResponse, ambiance);
      stepResponseBuilder.stepOutcome(
          StepResponse.StepOutcome.builder()
              .name(OutcomeExpressionConstants.TERRAFORM_OUTPUT)
              .outcome(TerraformApplyOutcome.builder()
                           .outputs(helper.parseTerraformOutputs(terraformTaskNGResponse.getOutputs()))
                           .build())
              .build());
    }
    return stepResponseBuilder.build();
  }

  private StepResponse handleTaskResultInherited(Ambiance ambiance, TerraformApplyStepParameters stepParameters,
      ThrowingSupplier<TerraformTaskNGResponse> responseSupplier) throws Exception {
    return null;
  }
}
