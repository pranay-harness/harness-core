package software.wings.sm.states.provision;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.ExceptionUtils.getMessage;

import static software.wings.beans.ARMSourceType.GIT;
import static software.wings.beans.TaskType.GIT_FETCH_FILES_TASK;
import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;
import static software.wings.beans.command.AzureARMCommandUnit.FetchFiles;
import static software.wings.delegatetasks.GitFetchFilesTask.GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT;

import static java.util.Collections.singletonList;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.tasks.Cd1SetupFields;
import io.harness.tasks.ResponseData;

import software.wings.api.ARMStateExecutionData;
import software.wings.api.ARMStateExecutionData.ARMStateExecutionDataBuilder;
import software.wings.beans.ARMInfrastructureProvisioner;
import software.wings.beans.Activity;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.TaskType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants(innerTypeName = "ARMProvisionStateKeys")
public class ARMProvisionState extends State {
  private static final String TEMPLATE_KEY = "TEMPLATE";
  private static final String VARIABLES_KEY = "VARIABLES";

  @Getter @Setter private String provisionerId;
  @Getter @Setter private String cloudProviderId;
  @Getter @Setter private String timeoutExpression;

  @Getter @Setter private String locationExpression;
  @Getter @Setter private String subscriptionExpression;
  @Getter @Setter private String resourceGroupExpression;
  @Getter @Setter private String managementGroupExpression;

  @Getter @Setter private String inlineVariablesExpression;
  @Getter @Setter private GitFileConfig variablesGitFileConfig;

  @Inject private ARMStateHelper helper;
  @Inject private DelegateService delegateService;
  @Inject private ActivityService activityService;

  public ARMProvisionState(String name) {
    super(name, StateType.ARM_PROVISION.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    ARMInfrastructureProvisioner provisioner = helper.getProvisioner(context.getAppId(), provisionerId);
    boolean executeGitTask = helper.executeGitTask(provisioner, variablesGitFileConfig);
    Activity activity = helper.createActivity(context, executeGitTask, getStateType());

    if (executeGitTask) {
      return executeGitTask(context, provisioner, activity);
    } else {
      return executeARMTask(context, null, activity.getUuid());
    }
  }

  private ExecutionResponse executeGitTask(
      ExecutionContext context, ARMInfrastructureProvisioner provisioner, Activity activity) {
    ARMStateExecutionDataBuilder builder = ARMStateExecutionData.builder();
    builder.taskType(GIT_FETCH_FILES_TASK);
    Map<String, GitFetchFilesConfig> filesConfigMap = new HashMap<>();
    if (GIT == provisioner.getSourceType()) {
      filesConfigMap.put(TEMPLATE_KEY, helper.createGitFetchFilesConfig(provisioner.getGitFileConfig(), context));
    }
    if (variablesGitFileConfig != null) {
      filesConfigMap.put(VARIABLES_KEY, helper.createGitFetchFilesConfig(variablesGitFileConfig, context));
    }
    GitFetchFilesTaskParams taskParams = GitFetchFilesTaskParams.builder()
                                             .activityId(activity.getUuid())
                                             .accountId(context.getAccountId())
                                             .appId(context.getAppId())
                                             .executionLogName(FetchFiles)
                                             .isFinalState(true)
                                             .appManifestKind(K8S_MANIFEST)
                                             .gitFetchFilesConfigMap(filesConfigMap)
                                             .containerServiceParams(null)
                                             .isBindTaskFeatureSet(false)
                                             .build();

    DelegateTask delegateTask =
        DelegateTask.builder()
            .uuid(generateUuid())
            .accountId(context.getAccountId())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, context.getAppId())
            .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, context.fetchRequiredEnvironment().getUuid())
            .setupAbstraction(Cd1SetupFields.ENV_TYPE_FIELD, context.getEnvType())
            .data(TaskData.builder()
                      .async(true)
                      .taskType(GIT_FETCH_FILES_TASK.name())
                      .parameters(new Object[] {taskParams})
                      .timeout(TimeUnit.MINUTES.toMillis(GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT))
                      .build())
            .build();
    delegateService.queueTask(delegateTask);
    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(singletonList(delegateTask.getUuid()))
        .stateExecutionData(builder.activityId(activity.getUuid()).build())
        .build();
  }

  private ExecutionResponse executeARMTask(
      ExecutionContext context, ARMStateExecutionData stateExecutionData, String activityId) {
    ARMStateExecutionDataBuilder builder = ARMStateExecutionData.builder();
    builder.taskType(TaskType.AZURE_ARM_TASK);
    builder.activityId(activityId);
    ARMInfrastructureProvisioner provisioner = helper.getProvisioner(context.getAppId(), provisionerId);
    if (stateExecutionData != null) {
      builder.fetchFilesResult(stateExecutionData.getFetchFilesResult());
    }
    String templateBody = null;
    if (GIT == provisioner.getSourceType()) {
      templateBody = helper.extractJsonFromGitResponse(stateExecutionData, TEMPLATE_KEY);
    } else {
      templateBody = provisioner.getTemplateBody();
      builder.inlineTemplateForRollback(templateBody);
    }

    String variablesBody = null;
    if (variablesGitFileConfig != null) {
      variablesBody = helper.extractJsonFromGitResponse(stateExecutionData, VARIABLES_KEY);
    } else {
      variablesBody = inlineVariablesExpression;
      builder.inlineVariablesForRollback(variablesBody);
    }

    throw new InvalidRequestException("Implement me to send ARM delegate task");
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    ARMStateExecutionData stateExecutionData = context.getStateExecutionData();
    TaskType taskType = stateExecutionData.getTaskType();
    switch (taskType) {
      case GIT_FETCH_FILES_TASK:
        return handleAsyncInternalGitTask(context, response, stateExecutionData);
      case AZURE_ARM_TASK:
        return handleAsyncInternalARMTask(context, response, stateExecutionData);
      default:
        throw new InvalidRequestException("Unhandled task type " + taskType);
    }
  }

  private ExecutionResponse handleAsyncInternalGitTask(
      ExecutionContext context, Map<String, ResponseData> response, ARMStateExecutionData stateExecutionData) {
    GitCommandExecutionResponse executionResponse = (GitCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        executionResponse.getGitCommandStatus() == GitCommandExecutionResponse.GitCommandStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    if (ExecutionStatus.FAILED == executionStatus) {
      activityService.updateStatus(stateExecutionData.getActivityId(), context.getAppId(), executionStatus);
      return ExecutionResponse.builder().executionStatus(executionStatus).build();
    }
    stateExecutionData.setFetchFilesResult(
        (GitFetchFilesFromMultipleRepoResult) executionResponse.getGitCommandResult());
    return executeARMTask(context, stateExecutionData, stateExecutionData.getActivityId());
  }

  private ExecutionResponse handleAsyncInternalARMTask(
      ExecutionContext context, Map<String, ResponseData> response, ARMStateExecutionData stateExecutionData) {
    // Save ARM outputs for use in Wf if success
    // Save ARM config if success
    throw new InvalidRequestException("Not implemented yet!!");
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // No implementation done yet for this method
  }
}
