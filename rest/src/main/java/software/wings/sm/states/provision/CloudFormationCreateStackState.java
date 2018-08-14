package software.wings.sm.states.provision;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.TaskType.CLOUD_FORMATION_TASK;

import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.cloudformation.CloudFormationElement;
import software.wings.api.cloudformation.CloudFormationOutputInfoElement;
import software.wings.beans.AwsConfig;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.DelegateTask;
import software.wings.beans.Log.Builder;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest.CloudFormationCommandType;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest.CloudFormationCreateStackRequestBuilder;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.service.intfc.AppService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateType;
import software.wings.sm.states.ManagerExecutionLogCallback;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    builder.stackNameSuffix(getStackNameSuffix(executionContext, provisioner))
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
    ScriptStateExecutionData scriptStateExecutionData = (ScriptStateExecutionData) context.getStateExecutionData();
    Builder logBuilder = aLog()
                             .withAppId(context.getAppId())
                             .withActivityId(scriptStateExecutionData.getActivityId())
                             .withLogLevel(LogLevel.INFO)
                             .withCommandUnitName(commandUnit())
                             .withExecutionResult(CommandExecutionStatus.RUNNING);

    ManagerExecutionLogCallback executionLogCallback =
        new ManagerExecutionLogCallback(logService, logBuilder, scriptStateExecutionData.getActivityId());

    if (CommandExecutionStatus.SUCCESS.equals(commandResponse.getCommandExecutionStatus())) {
      Map<String, Object> outputs = ((CloudFormationCreateStackResponse) commandResponse).getCloudFormationOutputMap();
      infrastructureProvisionerService.regenerateInfrastructureMappings(
          provisionerId, context, outputs, Optional.of(executionLogCallback), Optional.of(region));
      CloudFormationOutputInfoElement outputElement =
          CloudFormationOutputInfoElement.builder().newStackOutputs(outputs).build();
      return singletonList(outputElement);
    }
    return emptyList();
  }
}