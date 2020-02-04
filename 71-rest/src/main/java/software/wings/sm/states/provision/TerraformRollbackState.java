package software.wings.sm.states.provision;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.delegation.TerraformProvisionParameters.TIMEOUT_IN_MINUTES;
import static software.wings.service.intfc.FileService.FileBucket.TERRAFORM_STATE;

import com.google.inject.Inject;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.SweepingOutputInstance;
import io.harness.delegate.beans.ResponseData;
import io.harness.persistence.HIterator;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.api.TerraformApplyMarkerParam;
import software.wings.api.TerraformExecutionData;
import software.wings.beans.GitConfig;
import software.wings.beans.NameValuePair;
import software.wings.beans.PhaseStep;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommandUnit;
import software.wings.beans.infrastructure.TerraformConfig;
import software.wings.beans.infrastructure.TerraformConfig.TerraformConfigKeys;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.utils.GitUtilsManager;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@Slf4j
public class TerraformRollbackState extends TerraformProvisionState {
  private TerraformCommand rollbackCommand;

  @Inject private GitUtilsManager gitUtilsManager;

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public TerraformRollbackState(String name) {
    super(name, StateType.TERRAFORM_ROLLBACK.name());
  }

  @Override
  protected TerraformCommandUnit commandUnit() {
    return TerraformCommandUnit.Rollback;
  }

  @Override
  protected TerraformCommand command() {
    return rollbackCommand;
  }

  private boolean applyHappened(ExecutionContext context) {
    SweepingOutputInstance sweepingOutputInstance =
        sweepingOutputService.find(context.prepareSweepingOutputInquiryBuilder().name(getMarkerName()).build());
    if (sweepingOutputInstance == null) {
      return false;
    }
    return ((TerraformApplyMarkerParam) sweepingOutputInstance.getValue()).isApplyCompleted();
  }

  @Override
  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    TerraformInfrastructureProvisioner terraformProvisioner = getTerraformInfrastructureProvisioner(context);
    if (!applyHappened(context)) {
      return ExecutionResponse.builder()
          .executionStatus(SUCCESS)
          .errorMessage(format("Apply did not happen with provisioner: [%s]", terraformProvisioner.getName()))
          .build();
    }
    String path = context.renderExpression(terraformProvisioner.getPath());
    String workspace = context.renderExpression(getWorkspace());
    workspace = handleDefaultWorkspace(workspace);
    String entityId = generateEntityId(context, workspace);
    try (HIterator<TerraformConfig> configIterator =
             new HIterator(wingsPersistence.createQuery(TerraformConfig.class)
                               .filter(TerraformConfigKeys.appId, context.getAppId())
                               .filter(TerraformConfigKeys.entityId, entityId)
                               .order(Sort.descending(TerraformConfigKeys.createdAt))
                               .fetch())) {
      if (!configIterator.hasNext()) {
        return ExecutionResponse.builder()
            .executionStatus(SUCCESS)
            .errorMessage("No Rollback Required. Provisioning seems to have failed.")
            .build();
      }

      TerraformConfig configParameter = null;
      TerraformConfig currentConfig = null;
      while (configIterator.hasNext()) {
        configParameter = configIterator.next();

        if (configParameter.getWorkflowExecutionId().equals(context.getWorkflowExecutionId())) {
          if (currentConfig == null) {
            currentConfig = configParameter;
          }
        } else {
          TerraformCommand savedCommand = configParameter.getCommand();
          rollbackCommand = savedCommand != null ? savedCommand : TerraformCommand.APPLY;
          break;
        }
      }

      if (configParameter == currentConfig) {
        rollbackCommand = TerraformCommand.DESTROY;
      }

      final String fileId = fileService.getLatestFileId(entityId, TERRAFORM_STATE);
      notNullCheck("TerraformConfig cannot be null", configParameter);
      final GitConfig gitConfig = gitUtilsManager.getGitConfig(configParameter.getSourceRepoSettingId());
      if (StringUtils.isNotEmpty(configParameter.getSourceRepoReference())) {
        gitConfig.setReference(configParameter.getSourceRepoReference());
        String branch = context.renderExpression(terraformProvisioner.getSourceRepoBranch());
        if (isNotEmpty(branch)) {
          gitConfig.setBranch(branch);
        }
      }

      List<NameValuePair> allVariables = configParameter.getVariables();
      Map<String, String> textVariables = null;
      Map<String, EncryptedDataDetail> encryptedTextVariables = null;
      if (allVariables != null) {
        textVariables = infrastructureProvisionerService.extractTextVariables(allVariables, context);
        encryptedTextVariables =
            infrastructureProvisionerService.extractEncryptedTextVariables(allVariables, context.getAppId());
      }

      List<NameValuePair> allBackendConfigs = configParameter.getBackendConfigs();
      Map<String, String> backendConfigs = null;
      Map<String, EncryptedDataDetail> encryptedBackendConfigs = null;
      if (allBackendConfigs != null) {
        backendConfigs = infrastructureProvisionerService.extractTextVariables(allBackendConfigs, context);
        encryptedBackendConfigs =
            infrastructureProvisionerService.extractEncryptedTextVariables(allBackendConfigs, context.getAppId());
      }

      List<String> targets = configParameter.getTargets();
      targets = resolveTargets(targets, context);

      ExecutionContextImpl executionContext = (ExecutionContextImpl) context;
      TerraformProvisionParameters parameters =
          TerraformProvisionParameters.builder()
              .timeoutInMillis(defaultIfNullTimeout(TimeUnit.MINUTES.toMillis(TIMEOUT_IN_MINUTES)))
              .accountId(executionContext.getApp().getAccountId())
              .activityId(activityId)
              .appId(executionContext.getAppId())
              .currentStateFileId(fileId)
              .entityId(entityId)
              .command(rollbackCommand)
              .commandUnit(TerraformCommandUnit.Rollback)
              .sourceRepoSettingId(configParameter.getSourceRepoSettingId())
              .sourceRepo(gitConfig)
              .sourceRepoEncryptionDetails(secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null))
              .scriptPath(path)
              .variables(textVariables)
              .encryptedVariables(encryptedTextVariables)
              .backendConfigs(backendConfigs)
              .encryptedBackendConfigs(encryptedBackendConfigs)
              .targets(targets)
              .runPlanOnly(false)
              .tfVarFiles(configParameter.getTfVarFiles())
              .workspace(workspace)
              .delegateTag(configParameter.getDelegateTag())
              .build();

      return createAndRunTask(activityId, executionContext, parameters, configParameter.getDelegateTag());
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    Entry<String, ResponseData> entry = response.entrySet().iterator().next();
    TerraformExecutionData terraformExecutionData = (TerraformExecutionData) entry.getValue();
    TerraformInfrastructureProvisioner terraformProvisioner = getTerraformInfrastructureProvisioner(context);

    fileService.updateParentEntityIdAndVersion(PhaseStep.class, terraformExecutionData.getEntityId(), null,
        terraformExecutionData.getStateFileId(), null, FileBucket.TERRAFORM_STATE);
    if (terraformExecutionData.getExecutionStatus() == SUCCESS) {
      if (terraformExecutionData.getCommandExecuted() == TerraformCommand.APPLY) {
        saveTerraformConfig(context, terraformProvisioner, terraformExecutionData);
      } else if (terraformExecutionData.getCommandExecuted() == TerraformCommand.DESTROY) {
        Query<TerraformConfig> query =
            wingsPersistence.createQuery(TerraformConfig.class)
                .filter(TerraformConfigKeys.entityId, generateEntityId(context, terraformExecutionData.getWorkspace()))
                .filter(TerraformConfigKeys.workflowExecutionId, context.getWorkflowExecutionId());

        wingsPersistence.delete(query);
      }
    }

    return ExecutionResponse.builder().executionStatus(terraformExecutionData.getExecutionStatus()).build();
  }

  @Override
  @SchemaIgnore
  public String getWorkspace() {
    return super.getWorkspace();
  }

  @Override
  @SchemaIgnore
  public String getProvisionerId() {
    return super.getProvisionerId();
  }

  @Override
  @SchemaIgnore
  public Integer getTimeoutMillis() {
    return super.getTimeoutMillis();
  }
}