package software.wings.sm.states.provision;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.beans.TaskType.CLOUD_FORMATION_TASK;

import io.harness.delegate.beans.TaskData;
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

  protected DelegateTask buildDelegateTask(ExecutionContextImpl executionContext,
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
    setTimeOutOnRequest(request);
    return DelegateTask.builder()
        .async(true)
        .taskType(CLOUD_FORMATION_TASK.name())
        .accountId(executionContext.getApp().getAccountId())
        .waitId(activityId)
        .tags(isNotEmpty(request.getAwsConfig().getTag()) ? singletonList(request.getAwsConfig().getTag()) : null)
        .appId(executionContext.getApp().getUuid())
        .data(
            TaskData.builder()
                .parameters(new Object[] {request, secretManager.getEncryptionDetails(awsConfig, GLOBAL_APP_ID, null)})
                .timeout(defaultIfNullTimeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                .build())
        .build();
  }

  protected List<CloudFormationElement> handleResponse(
      CloudFormationCommandResponse commandResponse, ExecutionContext context) {
    return emptyList();
  }
}