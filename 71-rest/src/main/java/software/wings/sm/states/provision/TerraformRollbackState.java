package software.wings.sm.states.provision;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.TaskType.TERRAFORM_PROVISION_TASK;
import static software.wings.service.intfc.FileService.FileBucket.TERRAFORM_STATE;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.TerraformExecutionData;
import software.wings.beans.DelegateTask;
import software.wings.beans.GitConfig;
import software.wings.beans.NameValuePair;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommandUnit;
import software.wings.beans.infrastructure.TerraformfConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Getter
@Setter
public class TerraformRollbackState extends TerraformProvisionState {
  private static final Logger logger = LoggerFactory.getLogger(TerraformRollbackState.class);
  private TerraformCommand rollbackCommand;

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
    return null;
  }

  @Override
  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    TerraformInfrastructureProvisioner terraformProvisioner = getTerraformInfrastructureProvisioner(context);
    String path = context.renderExpression(terraformProvisioner.getPath());

    String entityId = generateEntityId(context);
    Iterator<TerraformfConfig> configIterator = wingsPersistence.createQuery(TerraformfConfig.class)
                                                    .filter(TerraformfConfig.ENTITY_ID_KEY, entityId)
                                                    .order(Sort.descending(TerraformfConfig.CREATED_AT_KEY))
                                                    .iterator();

    if (!configIterator.hasNext()) {
      return anExecutionResponse()
          .withExecutionStatus(SUCCESS)
          .withErrorMessage("No Rollback Required. Provisioning seems to have failed.")
          .build();
    }

    TerraformfConfig configParameter = null;
    TerraformfConfig currentConfig = null;
    while (configIterator.hasNext()) {
      configParameter = configIterator.next();

      if (configParameter.getWorkflowExecutionId().equals(context.getWorkflowExecutionId())) {
        if (currentConfig == null) {
          currentConfig = configParameter;
        }
      } else {
        rollbackCommand = TerraformCommand.APPLY;
        break;
      }
    }

    if (configParameter == currentConfig) {
      rollbackCommand = TerraformCommand.DESTROY;
    }

    final String fileId = fileService.getLatestFileId(entityId, TERRAFORM_STATE);
    final GitConfig gitConfig = getGitConfig(configParameter.getSourceRepoSettingId());
    if (StringUtils.isNotEmpty(configParameter.getSourceRepoReference())) {
      gitConfig.setReference(configParameter.getSourceRepoReference());
    }

    List<NameValuePair> allVariables = configParameter.getVariables();
    Map<String, String> textVariables = extractTextVariables(allVariables.stream(), context);
    Map<String, EncryptedDataDetail> encryptedTextVariables =
        extractEncryptedTextVariables(allVariables.stream(), context);

    List<NameValuePair> allBackendConfigs = configParameter.getBackendConfigs();
    Map<String, String> backendConfigs = null;
    Map<String, EncryptedDataDetail> encryptedBackendConfigs = null;
    if (allBackendConfigs != null) {
      backendConfigs = extractTextVariables(allBackendConfigs.stream(), context);
      encryptedBackendConfigs = extractEncryptedTextVariables(allBackendConfigs.stream(), context);
    }

    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;
    TerraformProvisionParameters parameters =
        TerraformProvisionParameters.builder()
            .accountId(executionContext.getApp().getAccountId())
            .activityId(activityId)
            .appId(executionContext.getAppId())
            .currentStateFileId(fileId)
            .entityId(entityId)
            .command(rollbackCommand)
            .commandUnit(TerraformCommandUnit.Rollback)
            .sourceRepo(gitConfig)
            .sourceRepoEncryptionDetails(secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null))
            .scriptPath(path)
            .variables(textVariables)
            .encryptedVariables(encryptedTextVariables)
            .backendConfigs(backendConfigs)
            .encryptedBackendConfigs(encryptedBackendConfigs)
            .build();

    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TERRAFORM_PROVISION_TASK)
                                    .withAccountId(executionContext.getApp().getAccountId())
                                    .withWaitId(activityId)
                                    .withAppId(((ExecutionContextImpl) context).getApp().getAppId())
                                    .withParameters(new Object[] {parameters})
                                    .build();

    if (getTimeoutMillis() != null) {
      delegateTask.setTimeout(getTimeoutMillis());
    }
    String delegateTaskId = delegateService.queueTask(delegateTask);

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .withStateExecutionData(ScriptStateExecutionData.builder().activityId(activityId).build())
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    Entry<String, ResponseData> entry = response.entrySet().iterator().next();
    TerraformExecutionData terraformExecutionData = (TerraformExecutionData) entry.getValue();
    TerraformInfrastructureProvisioner terraformProvisioner = getTerraformInfrastructureProvisioner(context);

    if (terraformExecutionData.getExecutionStatus() == SUCCESS) {
      if (terraformExecutionData.getCommandExecuted() == TerraformCommand.APPLY) {
        saveTerraformConfig(context, terraformProvisioner, terraformExecutionData);
      } else if (terraformExecutionData.getCommandExecuted() == TerraformCommand.DESTROY) {
        Query<TerraformfConfig> query =
            wingsPersistence.createQuery(TerraformfConfig.class)
                .filter(TerraformfConfig.ENTITY_ID_KEY, generateEntityId((ExecutionContextImpl) context))
                .filter(TerraformfConfig.WORKFLOW_EXECUTION_ID_KEY, context.getWorkflowExecutionId());

        wingsPersistence.delete(query);
      }
    }

    return anExecutionResponse().withExecutionStatus(terraformExecutionData.getExecutionStatus()).build();
  }
}
