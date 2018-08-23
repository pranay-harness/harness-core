package software.wings.sm.states.provision;

import static java.util.Collections.emptyList;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.TaskType.CLOUD_FORMATION_TASK;

import software.wings.api.cloudformation.CloudFormationElement;
import software.wings.beans.AwsConfig;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.DelegateTask;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest.CloudFormationCommandType;
import software.wings.helpers.ext.cloudformation.request.CloudFormationDeleteStackRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandResponse;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateType;

import java.util.List;

public class CloudFormationDeleteStackState extends CloudFormationState {
  private static final String COMMAND_UNIT = "Delete Stack";

  public CloudFormationDeleteStackState(String name) {
    super(name, StateType.CLOUD_FORMATION_DELETE_STACK.name());
  }

  protected String commandUnit() {
    return COMMAND_UNIT;
  }

  protected DelegateTask getDelegateTask(ExecutionContextImpl executionContext,
      CloudFormationInfrastructureProvisioner provisioner, AwsConfig awsConfig, String activityId) {
    CloudFormationDeleteStackRequest request =
        CloudFormationDeleteStackRequest.builder()
            .region(region)
            .stackNameSuffix(getStackNameSuffix(executionContext, provisioner.getUuid()))
            .commandType(CloudFormationCommandType.DELETE_STACK)
            .accountId(executionContext.getApp().getAccountId())
            .appId(executionContext.getApp().getUuid())
            .activityId(activityId)
            .commandName(commandUnit())
            .awsConfig(awsConfig)
            .build();
    return aDelegateTask()
        .withTaskType(CLOUD_FORMATION_TASK)
        .withAccountId(executionContext.getApp().getAccountId())
        .withWaitId(activityId)
        .withAppId(executionContext.getApp().getUuid())
        .withParameters(new Object[] {request, secretManager.getEncryptionDetails(awsConfig, GLOBAL_APP_ID, null)})
        .build();
  }

  protected List<CloudFormationElement> handleResponse(
      CloudFormationCommandResponse commandResponse, ExecutionContext context) {
    return emptyList();
  }
}