package software.wings.sm.states.provision;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.TaskType.CLOUD_FORMATION_TASK;
import static software.wings.sm.ContextElementType.CLOUD_FORMATION_ROLLBACK;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.cloudformation.CloudFormationElement;
import software.wings.api.cloudformation.CloudFormationRollbackInfoElement;
import software.wings.beans.AwsConfig;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.DelegateTask;
import software.wings.beans.NameValuePair;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest.CloudFormationCommandType;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest.CloudFormationCreateStackRequestBuilder;
import software.wings.helpers.ext.cloudformation.request.CloudFormationDeleteStackRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandResponse;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CloudFormationRollbackStackState extends CloudFormationState {
  private static final String COMMAND_UNIT = "Rollback Stack";

  public CloudFormationRollbackStackState(String name) {
    super(name, StateType.CLOUD_FORMATION_ROLLBACK_STACK.name());
  }

  protected String commandUnit() {
    return COMMAND_UNIT;
  }

  @Override
  @SchemaIgnore
  public String getProvisionerId() {
    return super.getProvisionerId();
  }

  @Override
  @SchemaIgnore
  public String getRegion() {
    return super.getRegion();
  }

  @Override
  @SchemaIgnore
  public String getAwsConfigId() {
    return super.getAwsConfigId();
  }

  @Override
  @SchemaIgnore
  public List<NameValuePair> getVariables() {
    return super.getVariables();
  }

  protected DelegateTask getDelegateTask(ExecutionContextImpl executionContext,
      CloudFormationInfrastructureProvisioner provisioner, AwsConfig awsConfig, String activityId) {
    throw new WingsException("Method should not be called");
  }

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  protected List<CloudFormationElement> handleResponse(
      CloudFormationCommandResponse commandResponse, ExecutionContext context) {
    CloudFormationRollbackInfoElement stackElement = context.getContextElement(CLOUD_FORMATION_ROLLBACK);
    if (stackElement.isStackExisted()) {
      updateInfraMappings(commandResponse, context, stackElement.getProvisionerId());
    }
    return emptyList();
  }

  private Optional<CloudFormationRollbackInfoElement> getRollbackElement(ExecutionContext context) {
    List<CloudFormationRollbackInfoElement> allRollbackElements =
        context.getContextElementList(CLOUD_FORMATION_ROLLBACK);
    if (isNotEmpty(allRollbackElements)) {
      return allRollbackElements.stream()
          .filter(element -> element.getProvisionerId().equals(provisionerId))
          .findFirst();
    }
    return Optional.empty();
  }

  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    Optional<CloudFormationRollbackInfoElement> stackElementOptional = getRollbackElement(context);
    if (!stackElementOptional.isPresent()) {
      return anExecutionResponse()
          .withExecutionStatus(SUCCESS)
          .withErrorMessage("No cloud formation rollback state found")
          .build();
    }
    CloudFormationRollbackInfoElement stackElement = stackElementOptional.get();
    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;
    AwsConfig awsConfig = getAwsConfig(stackElement.getAwsConfigId());
    DelegateTask delegateTask;
    if (!stackElement.isStackExisted()) {
      CloudFormationDeleteStackRequest request = CloudFormationDeleteStackRequest.builder()
                                                     .region(stackElement.getRegion())
                                                     .stackNameSuffix(stackElement.getStackNameSuffix())
                                                     .commandType(CloudFormationCommandType.DELETE_STACK)
                                                     .accountId(executionContext.getApp().getAccountId())
                                                     .appId(executionContext.getApp().getUuid())
                                                     .activityId(activityId)
                                                     .commandName(commandUnit())
                                                     .awsConfig(awsConfig)
                                                     .build();
      setTimeOutOnRequest(request);
      delegateTask = aDelegateTask()
                         .withTaskType(CLOUD_FORMATION_TASK)
                         .withAccountId(executionContext.getApp().getAccountId())
                         .withWaitId(activityId)
                         .withAppId(executionContext.getApp().getUuid())
                         .withParameters(
                             new Object[] {request, secretManager.getEncryptionDetails(awsConfig, GLOBAL_APP_ID, null)})
                         .build();
    } else {
      CloudFormationCreateStackRequestBuilder builder = CloudFormationCreateStackRequest.builder();
      builder.createType(CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY)
          .data(stackElement.getOldStackBody());
      builder.stackNameSuffix(stackElement.getStackNameSuffix())
          .region(stackElement.getRegion())
          .commandType(CloudFormationCommandType.CREATE_STACK)
          .accountId(executionContext.getApp().getAccountId())
          .appId(executionContext.getApp().getUuid())
          .activityId(activityId)
          .commandName(commandUnit())
          .variables(stackElement.getOldStackParameters())
          .awsConfig(awsConfig);
      CloudFormationCreateStackRequest request = builder.build();
      setTimeOutOnRequest(request);
      delegateTask = aDelegateTask()
                         .withTaskType(CLOUD_FORMATION_TASK)
                         .withAccountId(executionContext.getApp().getAccountId())
                         .withWaitId(activityId)
                         .withAppId(executionContext.getApp().getUuid())
                         .withParameters(
                             new Object[] {request, secretManager.getEncryptionDetails(awsConfig, GLOBAL_APP_ID, null)})
                         .build();
    }
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
}