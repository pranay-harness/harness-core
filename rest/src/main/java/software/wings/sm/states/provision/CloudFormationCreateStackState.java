package software.wings.sm.states.provision;

import static java.util.Collections.emptyList;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.TaskType.CLOUD_FORMATION_TASK;

import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.api.cloudformation.CloudFormationElement;
import software.wings.api.cloudformation.CloudFormationOutputInfoElement;
import software.wings.api.cloudformation.CloudFormationRollbackInfoElement;
import software.wings.beans.AwsConfig;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.DelegateTask;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest.CloudFormationCommandType;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest.CloudFormationCreateStackRequestBuilder;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo;
import software.wings.service.intfc.AppService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CloudFormationCreateStackState extends CloudFormationState {
  private static final String COMMAND_UNIT = "Create Stack";
  @Inject private transient AppService appService;

  public CloudFormationCreateStackState(String name) {
    super(name, StateType.CLOUD_FORMATION_CREATE_STACK.name());
  }

  protected String commandUnit() {
    return COMMAND_UNIT;
  }

  protected DelegateTask getDelegateTask(ExecutionContextImpl executionContext,
      CloudFormationInfrastructureProvisioner provisioner, AwsConfig awsConfig, String activityId) {
    CloudFormationCreateStackRequestBuilder builder = CloudFormationCreateStackRequest.builder();
    if (provisioner.provisionByUrl()) {
      builder.createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_URL)
          .data(provisioner.getTemplateFilePath());
    } else if (provisioner.provisionByBody()) {
      String templateBody = provisioner.getTemplateBody();
      String renderedTemplate = executionContext.renderExpression(templateBody);
      builder.createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY).data(renderedTemplate);
    } else {
      throw new WingsException("Create type is not set on cloud provisioner");
    }
    builder.stackNameSuffix(getStackNameSuffix(executionContext, provisioner.getUuid()))
        .region(region)
        .commandType(CloudFormationCommandType.CREATE_STACK)
        .accountId(executionContext.getApp().getAccountId())
        .appId(executionContext.getApp().getUuid())
        .activityId(activityId)
        .commandName(commandUnit())
        .variables(getVariableMap(provisioner, executionContext))
        .awsConfig(awsConfig);
    return aDelegateTask()
        .withTaskType(CLOUD_FORMATION_TASK)
        .withAccountId(executionContext.getApp().getAccountId())
        .withWaitId(activityId)
        .withAppId(executionContext.getApp().getUuid())
        .withParameters(
            new Object[] {builder.build(), secretManager.getEncryptionDetails(awsConfig, GLOBAL_APP_ID, null)})
        .build();
  }
  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  protected List<CloudFormationElement> handleResponse(
      CloudFormationCommandResponse commandResponse, ExecutionContext context) {
    CloudFormationCreateStackResponse createStackResponse = (CloudFormationCreateStackResponse) commandResponse;
    if (CommandExecutionStatus.SUCCESS.equals(commandResponse.getCommandExecutionStatus())) {
      updateInfraMappings(commandResponse, context, provisionerId);
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
              .awsConfigId(awsConfigId)
              .region(region)
              .stackNameSuffix(getStackNameSuffix((ExecutionContextImpl) context, provisionerId))
              .oldStackBody(existingStackInfo.getOldStackBody())
              .oldStackParameters(existingStackInfo.getOldStackParameters())
              .build();
      return Arrays.asList(rollbackElement, outputElement);
    }
    return emptyList();
  }
}