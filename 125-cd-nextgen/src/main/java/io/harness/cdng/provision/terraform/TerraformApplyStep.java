package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.beans.FeatureName.EXPORT_TF_PLAN;
import static io.harness.cdng.manifest.ManifestType.TerraformConfig;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.FileBucket.TERRAFORM_STATE;
import static io.harness.delegate.task.terraform.TFTaskType.APPLY;
import static io.harness.delegate.task.terraform.TerraformTaskNGParameters.COMMAND_UNIT;
import static io.harness.delegate.task.terraform.TerraformTaskNGParameters.TerraformTaskNGParametersBuilder;
import static io.harness.delegate.task.terraform.TerraformTaskNGParameters.builder;
import static io.harness.ngpipeline.common.ParameterFieldHelper.getParameterFieldValue;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ff.FeatureFlagService;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
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

@OwnedBy(CDP)
@Slf4j
public class TerraformApplyStep implements TaskExecutable<TerraformApplyStepParameters, TerraformTaskNGResponse> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.TERRAFORM_APPLY.getYamlType()).build();

  private static final String TF_CONFIG_FILES = "TF_CONFIG_FILES";
  private static final String TF_VAR_FILES = "TF_VAR_FILES_%d";

  @Inject private KryoSerializer kryoSerializer;
  @Inject private TerraformStepHelper helper;
  @Inject private FileService fileService;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public Class<TerraformApplyStepParameters> getStepParametersClass() {
    return TerraformApplyStepParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, TerraformApplyStepParameters stepParameters, StepInputPackage inputPackage) {
    TerraformStepConfigurationType configurationType = stepParameters.getStepConfigurationType();
    switch (configurationType) {
      case INLINE:
        return obtainInlineTask(ambiance, stepParameters);
      case INHERIT_FROM_PLAN:
        return obtainInheritedTask(ambiance, stepParameters);
      default:
        throw new InvalidRequestException(
            String.format("Unknown configuration Type: [%s]", configurationType.getDisplayName()));
    }
  }

  private TaskRequest obtainInlineTask(Ambiance ambiance, TerraformApplyStepParameters stepParameters) {
    TerraformTaskNGParametersBuilder builder = builder();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    builder.accountId(accountId);
    String entityId = helper.generateFullIdentifier(stepParameters.getProvisionerIdentifier(), ambiance);
    builder.currentStateFileId(fileService.getLatestFileId(entityId, TERRAFORM_STATE));
    builder.taskType(APPLY);
    builder.provisionerIdentifier(stepParameters.getProvisionerIdentifier());
    builder.workspace(getParameterFieldValue(stepParameters.getWorkspace()));
    builder.configFiles(helper.gitFetchFilesConfig(
        stepParameters.getConfigFilesWrapper().getStoreConfig(), ambiance, TF_CONFIG_FILES, TerraformConfig));
    builder.inlineVarFiles(getParameterFieldValue(stepParameters.getInlineVarFiles()));
    if (isNotEmpty(stepParameters.getRemoteVarFiles())) {
      List<GitFetchFilesConfig> varFilesConfig = new ArrayList<>();
      int i = 1;
      for (StoreConfigWrapper varFileWrapper : stepParameters.getRemoteVarFiles()) {
        varFilesConfig.add(helper.gitFetchFilesConfig(
            varFileWrapper.getStoreConfig(), ambiance, String.format(TF_VAR_FILES, i), ManifestType.TerraformVarFile));
        i++;
      }
      builder.remoteVarfiles(varFilesConfig);
    }
    builder.backendConfig(getParameterFieldValue(stepParameters.getBackendConfig()));
    builder.targets(getParameterFieldValue(stepParameters.getTargets()));
    builder.saveTerraformStateJson(featureFlagService.isEnabled(EXPORT_TF_PLAN, accountId));

    // ToDo: Add env variables once added by @rohit

    // Todo: Timeout is on Step Element Config. Need to set it on Task Data.
    TaskData taskData = TaskData.builder()
                            .async(true)
                            .taskType(TaskType.TERRAFORM_TASK_NG.name())
                            .parameters(new Object[] {builder.build()})
                            .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer, Collections.singletonList(COMMAND_UNIT),
        TaskType.TERRAFORM_TASK_NG.getDisplayName());
  }

  private TaskRequest obtainInheritedTask(Ambiance ambiance, TerraformApplyStepParameters stepParameters) {
    TerraformTaskNGParametersBuilder builder = builder();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    builder.accountId(accountId);
    String entityId = helper.generateFullIdentifier(stepParameters.getProvisionerIdentifier(), ambiance);
    builder.currentStateFileId(fileService.getLatestFileId(entityId, TERRAFORM_STATE));
    builder.taskType(APPLY);
    builder.provisionerIdentifier(stepParameters.getProvisionerIdentifier());
    TerraformInheritOutput inheritOutput =
        helper.getSavedInheritOutput(stepParameters.getProvisionerIdentifier(), ambiance);
    builder.workspace(inheritOutput.getWorkspace());
    builder.configFiles(
        helper.gitFetchFilesConfig(inheritOutput.getConfigFiles(), ambiance, TF_CONFIG_FILES, TerraformConfig));
    if (isNotEmpty(inheritOutput.getRemoteVarFiles())) {
      List<GitFetchFilesConfig> varFilesConfig = new ArrayList<>();
      int i = 1;
      for (StoreConfig storeConfig : inheritOutput.getRemoteVarFiles()) {
        varFilesConfig.add(helper.gitFetchFilesConfig(
            storeConfig, ambiance, String.format(TF_VAR_FILES, i), ManifestType.TerraformVarFile));
        i++;
      }
      builder.remoteVarfiles(varFilesConfig);
    }
    builder.inlineVarFiles(inheritOutput.getInlineVarFiles());
    builder.backendConfig(inheritOutput.getBackendConfig());
    builder.targets(inheritOutput.getTargets());
    builder.saveTerraformStateJson(featureFlagService.isEnabled(EXPORT_TF_PLAN, accountId));
    builder.encryptionConfig(inheritOutput.getEncryptionConfig());
    builder.encryptedTfPlan(inheritOutput.getEncryptedTfPlan());
    builder.planName(inheritOutput.getPlanName());

    // ToDo: Add env variables once added by @rohit

    // Todo: Timeout is on Step Element Config. Need to set it on Task Data.
    TaskData taskData = TaskData.builder()
                            .async(true)
                            .taskType(TaskType.TERRAFORM_TASK_NG.name())
                            .parameters(new Object[] {builder.build()})
                            .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer, Collections.singletonList(COMMAND_UNIT),
        TaskType.TERRAFORM_TASK_NG.getDisplayName());
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, TerraformApplyStepParameters stepParameters,
      ThrowingSupplier<TerraformTaskNGResponse> responseSupplier) throws Exception {
    return null;
  }
}
