package software.wings.sm.states.provision;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.TaskType.CLOUD_FORMATION_TASK;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.StringUtils;
import software.wings.api.cloudformation.CloudFormationElement;
import software.wings.api.cloudformation.CloudFormationOutputInfoElement;
import software.wings.api.cloudformation.CloudFormationRollbackInfoElement;
import software.wings.beans.AwsConfig;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.GitConfig;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest.CloudFormationCommandType;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest.CloudFormationCreateStackRequestBuilder;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo;
import software.wings.service.intfc.AppService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateType;
import software.wings.utils.GitUtilsManager;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CloudFormationCreateStackState extends CloudFormationState {
  private static final String COMMAND_UNIT = "Create Stack";
  @Inject private transient AppService appService;
  @Inject private transient GitUtilsManager gitUtilsManager;

  public CloudFormationCreateStackState(String name) {
    super(name, StateType.CLOUD_FORMATION_CREATE_STACK.name());
  }

  @Override
  protected String commandUnit() {
    return COMMAND_UNIT;
  }

  private void ensureNonEmptyStringField(String field, String fieldName) {
    if (isEmpty(field)) {
      throw new InvalidRequestException(format("Field: [%s] in provisioner is required", fieldName));
    }
  }

  @Override
  protected DelegateTask buildDelegateTask(ExecutionContextImpl executionContext,
      CloudFormationInfrastructureProvisioner provisioner, AwsConfig awsConfig, String activityId) {
    CloudFormationCreateStackRequestBuilder builder = CloudFormationCreateStackRequest.builder();
    if (provisioner.provisionByUrl()) {
      ensureNonEmptyStringField(provisioner.getTemplateFilePath(), "Template Url");
      builder.createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_URL)
          .data(executionContext.renderExpression(provisioner.getTemplateFilePath()));
    } else if (provisioner.provisionByBody()) {
      String templateBody = provisioner.getTemplateBody();
      ensureNonEmptyStringField(templateBody, "Template Body");
      String renderedTemplate = executionContext.renderExpression(templateBody);
      builder.createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY).data(renderedTemplate);
    } else if (provisioner.provisionByGit()) {
      String sourceRepoSettingId = provisioner.getGitFileConfig().getConnectorId();
      GitConfig gitConfig = gitUtilsManager.getGitConfig(sourceRepoSettingId);
      String branch = provisioner.getGitFileConfig().getBranch();
      ensureNonEmptyStringField(sourceRepoSettingId, "sourceRepoSettingId");
      if (isNotEmpty(branch)) {
        gitConfig.setBranch(branch);
      }
      gitConfig.setReference(provisioner.getGitFileConfig().getCommitId());
      builder.createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_GIT)
          .gitFileConfig(provisioner.getGitFileConfig())
          .encryptedDataDetails(secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null))
          .gitConfig(gitConfig);
    } else {
      throw new WingsException("Create type is not set on cloud provisioner");
    }
    builder.stackNameSuffix(getStackNameSuffix(executionContext, provisioner.getUuid()))
        .customStackName(useCustomStackName ? executionContext.renderExpression(customStackName) : StringUtils.EMPTY)
        .region(region)
        .commandType(CloudFormationCommandType.CREATE_STACK)
        .accountId(executionContext.getApp().getAccountId())
        .appId(executionContext.getApp().getUuid())
        .activityId(activityId)
        .commandName(commandUnit())
        .variables(infrastructureProvisionerService.extractTextVariables(getVariables(), executionContext))
        .encryptedVariables(
            infrastructureProvisionerService.extractEncryptedTextVariables(getVariables(), executionContext.getAppId()))
        .awsConfig(awsConfig);
    CloudFormationCreateStackRequest request = builder.build();
    setTimeOutOnRequest(request);
    return DelegateTask.builder()
        .accountId(executionContext.getApp().getAccountId())
        .waitId(activityId)
        .tags(isNotEmpty(request.getAwsConfig().getTag()) ? singletonList(request.getAwsConfig().getTag()) : null)
        .appId(executionContext.getApp().getUuid())
        .data(
            TaskData.builder()
                .async(true)
                .taskType(CLOUD_FORMATION_TASK.name())
                .parameters(new Object[] {request, secretManager.getEncryptionDetails(awsConfig, GLOBAL_APP_ID, null)})
                .timeout(defaultIfNullTimeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                .build())
        .build();
  }

  @Override
  protected List<CloudFormationElement> handleResponse(
      CloudFormationCommandResponse commandResponse, ExecutionContext context) {
    CloudFormationCreateStackResponse createStackResponse = (CloudFormationCreateStackResponse) commandResponse;
    if (CommandExecutionStatus.SUCCESS == commandResponse.getCommandExecutionStatus()) {
      updateInfraMappings(commandResponse, context, provisionerId);
      saveCloudFormationRollbackConfig(
          createStackResponse.getRollbackInfo(), (ExecutionContextImpl) context, fetchResolvedAwsConfigId(context));
      Map<String, Object> outputs = ((CloudFormationCreateStackResponse) commandResponse).getCloudFormationOutputMap();
      CloudFormationOutputInfoElement outputElement =
          context.getContextElement(ContextElementType.CLOUD_FORMATION_PROVISION);
      if (outputElement == null) {
        outputElement = CloudFormationOutputInfoElement.builder().newStackOutputs(outputs).build();
      } else {
        outputElement.mergeOutputs(outputs);
      }
      ExistingStackInfo existingStackInfo = createStackResponse.getExistingStackInfo();
      CloudFormationRollbackInfoElement rollbackElement =
          CloudFormationRollbackInfoElement.builder()
              .stackExisted(existingStackInfo.isStackExisted())
              .provisionerId(provisionerId)
              .awsConfigId(fetchResolvedAwsConfigId(context))
              .region(region)
              .stackNameSuffix(getStackNameSuffix((ExecutionContextImpl) context, provisionerId))
              .customStackName(useCustomStackName ? context.renderExpression(customStackName) : StringUtils.EMPTY)
              .oldStackBody(existingStackInfo.getOldStackBody())
              .oldStackParameters(existingStackInfo.getOldStackParameters())
              .build();
      return Arrays.asList(rollbackElement, outputElement);
    }
    return emptyList();
  }
}