package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.InstanceElement;
import software.wings.api.JenkinsExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsSubTaskType;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.command.JenkinsTaskParams;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.mixin.SweepingOutputStateMixin;
import software.wings.stencils.DefaultValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class JenkinsState extends State implements SweepingOutputStateMixin {
  public static final String COMMAND_UNIT_NAME = "Console Output";
  public static final String JENKINS_CONFIG_ID_KEY = "jenkinsConfigId";
  public static final String JOB_NAME_KEY = "jobName";
  public static final String SWEEPING_OUTPUT_NAME_KEY = "sweepingOutputName";
  public static final String SWEEPING_OUTPUT_SCOPE_KEY = "sweepingOutputScope";

  private String jenkinsConfigId;

  private String jobName;
  @Getter @Setter private boolean jobNameAsExpression;

  private List<ParameterEntry> jobParameters = Lists.newArrayList();

  private boolean unstableSuccess;

  private boolean injectEnvVars;

  private List<FilePathAssertionEntry> filePathsForAssertion = Lists.newArrayList();

  @Getter @Setter private String sweepingOutputName;
  @Getter @Setter private SweepingOutputInstance.Scope sweepingOutputScope;

  @Transient @Inject private DelegateService delegateService;
  @Transient @Inject private ActivityService activityService;
  @Transient @Inject private SecretManager secretManager;
  @Transient @Inject private SweepingOutputService sweepingOutputService;

  public JenkinsState(String name) {
    super(name, StateType.JENKINS.name());
  }

  /**
   * Getter for property 'jenkinsConfigId'.
   *
   * @return Value for property 'jenkinsConfigId'.
   */
  public String getJenkinsConfigId() {
    return jenkinsConfigId;
  }

  /**
   * Setter for property 'jenkinsConfigId'.
   *
   * @param jenkinsConfigId Value to set for property 'jenkinsConfigId'.
   */
  public void setJenkinsConfigId(String jenkinsConfigId) {
    this.jenkinsConfigId = jenkinsConfigId;
  }

  /**
   * Getter for property 'jobName'.
   *
   * @return Value for property 'jobName'.
   */
  public String getJobName() {
    return jobName;
  }

  /**
   * Setter for property 'jobName'.
   *
   * @param jobName Value to set for property 'jobName'.
   */
  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  /**
   * Getter for property 'jobParameters'.
   *
   * @return Value for property 'jobParameters'.
   */
  public List<ParameterEntry> getJobParameters() {
    return jobParameters;
  }

  /**
   * Setter for property 'jobParameters'.
   *
   * @param jobParameters Value to set for property 'jobParameters'.
   */
  public void setJobParameters(List<ParameterEntry> jobParameters) {
    this.jobParameters = jobParameters;
  }

  /**
   * Getter for property 'filePathsForAssertion'.
   *
   * @return Value for property 'filePathsForAssertion'.
   */
  public List<FilePathAssertionEntry> getFilePathsForAssertion() {
    return filePathsForAssertion;
  }

  /**
   * Setter for property 'filePathsForAssertion'.
   *
   * @param filePathsForAssertion Value to set for property 'filePathsForAssertion'.
   */
  public void setFilePathsForAssertion(List<FilePathAssertionEntry> filePathsForAssertion) {
    this.filePathsForAssertion = filePathsForAssertion;
  }

  public boolean isUnstableSuccess() {
    return unstableSuccess;
  }

  public void setUnstableSuccess(boolean unstableSuccess) {
    this.unstableSuccess = unstableSuccess;
  }

  public boolean isInjectEnvVars() {
    return injectEnvVars;
  }

  public void setInjectEnvVars(boolean injectEnvVars) {
    this.injectEnvVars = injectEnvVars;
  }

  @Override
  @Attributes(title = "Wait interval before execution (s)")
  public Integer getWaitInterval() {
    return super.getWaitInterval();
  }

  @Attributes(title = "Execute with previous steps")
  public boolean getExecuteWithPreviousSteps() {
    return super.isExecuteWithPreviousSteps();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    return executeInternal(context, activityId);
  }

  /**
   * Execute internal execution response.
   *
   * @param context the context
   * @return the execution response
   */
  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    String envId = (workflowStandardParams == null || workflowStandardParams.getEnv() == null)
        ? null
        : workflowStandardParams.getEnv().getUuid();

    String accountId = ((ExecutionContextImpl) context).fetchRequiredApp().getAccountId();

    JenkinsConfig jenkinsConfig = (JenkinsConfig) context.getGlobalSettingValue(accountId, jenkinsConfigId);
    if (jenkinsConfig == null) {
      logger.warn("JenkinsConfig Id {} does not exist. It might have been deleted", jenkinsConfigId);
      return ExecutionResponse.builder()
          .executionStatus(FAILED)
          .errorMessage("Jenkins Server was deleted. Please update with an appropriate server.")
          .build();
    }

    String evaluatedJobName = context.renderExpression(jobName);

    Map<String, String> jobParameterMap = isEmpty(jobParameters)
        ? Collections.emptyMap()
        : jobParameters.stream().collect(toMap(ParameterEntry::getKey, ParameterEntry::getValue));

    Map<String, String> evaluatedParameters = Maps.newHashMap(jobParameterMap);
    evaluatedParameters.forEach(
        (String key, String value) -> evaluatedParameters.put(key, context.renderExpression(value)));

    Map<String, String> evaluatedFilePathsForAssertion = Maps.newHashMap();
    if (isNotEmpty(filePathsForAssertion)) {
      filePathsForAssertion.forEach(filePathAssertionEntry
          -> evaluatedFilePathsForAssertion.put(
              context.renderExpression(filePathAssertionEntry.getFilePath()), filePathAssertionEntry.getAssertion()));
    }

    String infrastructureMappingId = context.fetchInfraMappingId();
    JenkinsTaskParams jenkinsTaskParams = JenkinsTaskParams.builder()
                                              .jenkinsConfig(jenkinsConfig)
                                              .encryptedDataDetails(secretManager.getEncryptionDetails(
                                                  jenkinsConfig, context.getAppId(), context.getWorkflowExecutionId()))
                                              .jobName(evaluatedJobName)
                                              .parameters(evaluatedParameters)
                                              .filePathsForAssertion(evaluatedFilePathsForAssertion)
                                              .activityId(activityId)
                                              .unitName(COMMAND_UNIT_NAME)
                                              .unstableSuccess(unstableSuccess)
                                              .injectEnvVars(injectEnvVars)
                                              .subTaskType(JenkinsSubTaskType.START_TASK)
                                              .queuedBuildUrl(null)
                                              .build();

    DelegateTask delegateTask =
        buildDelegateTask(context, activityId, jenkinsTaskParams, envId, infrastructureMappingId);

    if (getTimeoutMillis() != null) {
      jenkinsTaskParams.setTimeout(getTimeoutMillis());
      jenkinsTaskParams.setStartTs(System.currentTimeMillis());
    }
    String delegateTaskId = delegateService.queueTask(delegateTask);

    JenkinsExecutionData jenkinsExecutionData = JenkinsExecutionData.builder()
                                                    .jobName(evaluatedJobName)
                                                    .jobParameters(evaluatedParameters)
                                                    .activityId(activityId)
                                                    .build();
    return ExecutionResponse.builder()
        .async(true)
        .stateExecutionData(jenkinsExecutionData)
        .correlationIds(Collections.singletonList(activityId))
        .delegateTaskId(delegateTaskId)
        .build();
  }

  protected TaskType getTaskType() {
    return TaskType.JENKINS;
  }

  private DelegateTask buildDelegateTask(ExecutionContext context, String activityId,
      JenkinsTaskParams jenkinsTaskParams, String envId, String infrastructureMappingId) {
    return DelegateTask.builder()
        .async(true)
        .accountId(((ExecutionContextImpl) context).fetchRequiredApp().getAccountId())
        .waitId(activityId)
        .appId(((ExecutionContextImpl) context).fetchRequiredApp().getAppId())
        .data(TaskData.builder()
                  .taskType(getTaskType().name())
                  .parameters(new Object[] {jenkinsTaskParams})
                  .timeout(defaultIfNullTimeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                  .build())
        .envId(envId)
        .infrastructureMappingId(infrastructureMappingId)
        .build();
  }

  public ExecutionResponse startJenkinsPollTask(ExecutionContext context, Map<String, ResponseData> response) {
    JenkinsExecutionResponse jenkinsExecutionResponse = (JenkinsExecutionResponse) response.values().iterator().next();

    if (isEmpty(jenkinsExecutionResponse.queuedBuildUrl)) {
      return ExecutionResponse.builder().async(true).executionStatus(FAILED).build();
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    String envId = (workflowStandardParams == null || workflowStandardParams.getEnv() == null)
        ? null
        : workflowStandardParams.getEnv().getUuid();

    String infrastructureMappingId = context.fetchInfraMappingId();

    String accountId = ((ExecutionContextImpl) context).fetchRequiredApp().getAccountId();
    JenkinsConfig jenkinsConfig = (JenkinsConfig) context.getGlobalSettingValue(accountId, jenkinsConfigId);
    if (jenkinsConfig == null) {
      logger.warn("JenkinsConfig Id {} does not exist. It might have been deleted", jenkinsConfigId);
      return ExecutionResponse.builder()
          .executionStatus(FAILED)
          .errorMessage("Jenkins Server was deleted. Please update with an appropriate server.")
          .build();
    }

    JenkinsTaskParams jenkinsTaskParams = JenkinsTaskParams.builder()
                                              .jenkinsConfig(jenkinsConfig)
                                              .encryptedDataDetails(secretManager.getEncryptionDetails(
                                                  jenkinsConfig, context.getAppId(), context.getWorkflowExecutionId()))
                                              .activityId(jenkinsExecutionResponse.activityId)
                                              .unitName(COMMAND_UNIT_NAME)
                                              .unstableSuccess(unstableSuccess)
                                              .injectEnvVars(injectEnvVars)
                                              .subTaskType(JenkinsSubTaskType.POLL_TASK)
                                              .queuedBuildUrl(jenkinsExecutionResponse.queuedBuildUrl)
                                              .build();

    String waitId = UUIDGenerator.generateUuid();
    DelegateTask delegateTask = buildDelegateTask(context, waitId, jenkinsTaskParams, envId, infrastructureMappingId);

    if (getTimeoutMillis() != null) {
      jenkinsTaskParams.setStartTs(System.currentTimeMillis());
      // Set remaining time for Poll task plus 2 minutes
      jenkinsTaskParams.setTimeout(getTimeoutMillis() - jenkinsExecutionResponse.getTimeElapsed() + 120000);
    }

    String delegateTaskId = delegateService.queueTask(delegateTask);
    JenkinsExecutionData jenkinsExecutionData = (JenkinsExecutionData) context.getStateExecutionData();
    jenkinsExecutionData.setActivityId(jenkinsExecutionResponse.activityId);
    jenkinsExecutionData.setJobStatus(jenkinsExecutionResponse.getJenkinsResult());
    jenkinsExecutionData.setBuildUrl(jenkinsExecutionResponse.getJobUrl());
    jenkinsExecutionData.setBuildNumber(jenkinsExecutionResponse.getBuildNumber());
    jenkinsExecutionData.setBuildDisplayName(jenkinsExecutionResponse.getBuildDisplayName());
    jenkinsExecutionData.setBuildFullDisplayName(jenkinsExecutionResponse.getBuildFullDisplayName());

    return ExecutionResponse.builder()
        .async(true)
        .stateExecutionData(jenkinsExecutionData)
        .correlationIds(Collections.singletonList(waitId))
        .delegateTaskId(delegateTaskId)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ResponseData notifyResponseData = response.values().iterator().next();
    JenkinsExecutionResponse jenkinsExecutionResponse;
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      return ExecutionResponse.builder()
          .executionStatus(FAILED)
          .errorMessage(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage())
          .build();
    }

    jenkinsExecutionResponse = (JenkinsExecutionResponse) notifyResponseData;
    JenkinsExecutionData jenkinsExecutionData = (JenkinsExecutionData) context.getStateExecutionData();

    jenkinsExecutionData.setFilePathAssertionMap(jenkinsExecutionResponse.getFilePathAssertionMap());
    jenkinsExecutionData.setJobStatus(jenkinsExecutionResponse.getJenkinsResult());
    jenkinsExecutionData.setErrorMsg(jenkinsExecutionResponse.getErrorMessage());
    jenkinsExecutionData.setBuildUrl(jenkinsExecutionResponse.getJobUrl());
    jenkinsExecutionData.setBuildNumber(jenkinsExecutionResponse.getBuildNumber());
    jenkinsExecutionData.setBuildDisplayName(jenkinsExecutionResponse.getBuildDisplayName());
    jenkinsExecutionData.setBuildFullDisplayName(jenkinsExecutionResponse.getBuildFullDisplayName());
    jenkinsExecutionData.setDescription(jenkinsExecutionResponse.getDescription());
    jenkinsExecutionData.setMetadata(jenkinsExecutionResponse.getMetadata());
    jenkinsExecutionData.setEnvVars(jenkinsExecutionResponse.getEnvVars());

    // Async response for START_TASK received, start POLL_TASK
    if (isNotEmpty(jenkinsExecutionResponse.getSubTaskType().name())
        && jenkinsExecutionResponse.getSubTaskType().name().equals(JenkinsSubTaskType.START_TASK.name())) {
      if (SUCCESS != jenkinsExecutionResponse.getExecutionStatus()) {
        updateActivityStatus(jenkinsExecutionResponse.getActivityId(), ((ExecutionContextImpl) context).getAppId(),
            jenkinsExecutionResponse.getExecutionStatus());
        return ExecutionResponse.builder()
            .executionStatus(jenkinsExecutionResponse.getExecutionStatus())
            .stateExecutionData(jenkinsExecutionData)
            .errorMessage(jenkinsExecutionResponse.getErrorMessage())
            .build();
      }
      if (isNotEmpty(jenkinsExecutionResponse.getQueuedBuildUrl())) {
        // Set time taken for Start Task
        updateActivityStatus(jenkinsExecutionResponse.getActivityId(),
            ((ExecutionContextImpl) context).fetchRequiredApp().getUuid(), RUNNING);
        jenkinsExecutionResponse.setTimeElapsed(System.currentTimeMillis() - jenkinsExecutionData.getStartTs());
        return startJenkinsPollTask(context, response);
      } else {
        // Can not start POLL_TASK
        logger.error("Jenkins Queued Build URL is empty and could not start POLL_TASK", USER);
        updateActivityStatus(jenkinsExecutionResponse.getActivityId(),
            ((ExecutionContextImpl) context).fetchRequiredApp().getUuid(), FAILED);
        return ExecutionResponse.builder()
            .executionStatus(FAILED)
            .stateExecutionData(jenkinsExecutionData)
            .errorMessage("Queued build URL is empty. Verify in Jenkins server.")
            .build();
      }
    } else {
      updateActivityStatus(jenkinsExecutionResponse.getActivityId(),
          ((ExecutionContextImpl) context).fetchRequiredApp().getUuid(), jenkinsExecutionResponse.getExecutionStatus());
    }

    handleSweepingOutput(sweepingOutputService, context, jenkinsExecutionData);

    return ExecutionResponse.builder()
        .executionStatus(jenkinsExecutionResponse.getExecutionStatus())
        .stateExecutionData(jenkinsExecutionData)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    if (context == null || context.getStateExecutionData() == null) {
      return;
    }
    context.getStateExecutionData().setErrorMsg(
        "Job did not complete within timeout " + (getTimeoutMillis() / 1000) + " (s)");
  }

  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + DEFAULT_ASYNC_CALL_TIMEOUT)
  @Override
  public Integer getTimeoutMillis() {
    if (super.getTimeoutMillis() == null) {
      return Math.toIntExact(DEFAULT_ASYNC_CALL_TIMEOUT);
    }
    return super.getTimeoutMillis();
  }

  @Override
  @SchemaIgnore
  public List<String> getPatternsForRequiredContextElementType() {
    List<String> patterns = new ArrayList<>();
    patterns.add(jobName);
    if (isNotEmpty(jobParameters)) {
      jobParameters.forEach(parameterEntry -> patterns.add(parameterEntry.getValue()));
    }

    if (isNotEmpty(filePathsForAssertion)) {
      filePathsForAssertion.forEach(filePathAssertionEntry -> patterns.add(filePathAssertionEntry.getFilePath()));
    }
    return patterns;
  }

  protected String createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).fetchRequiredApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .commandName(getName())
                                          .type(Type.Verification)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(Collections.emptyList())
                                          .status(RUNNING)
                                          .commandUnitType(CommandUnitType.JENKINS)
                                          .triggeredBy(TriggeredBy.builder()
                                                           .email(workflowStandardParams.getCurrentUser().getEmail())
                                                           .name(workflowStandardParams.getCurrentUser().getName())
                                                           .build());

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType() == BUILD) {
      activityBuilder.environmentId(GLOBAL_ENV_ID).environmentName(GLOBAL_ENV_ID).environmentType(ALL);
    } else if (env != null) {
      activityBuilder.environmentId(env.getUuid())
          .environmentName(env.getName())
          .environmentType(env.getEnvironmentType());
    }
    if (instanceElement != null) {
      activityBuilder.serviceTemplateId(instanceElement.getServiceTemplateElement().getUuid())
          .serviceTemplateName(instanceElement.getServiceTemplateElement().getName())
          .serviceId(instanceElement.getServiceTemplateElement().getServiceElement().getUuid())
          .serviceName(instanceElement.getServiceTemplateElement().getServiceElement().getName())
          .serviceInstanceId(instanceElement.getUuid())
          .hostName(instanceElement.getHost().getHostName());
    }

    Activity activity = activityBuilder.build();
    activity.setAppId(app.getUuid());
    return activityService.save(activity).getUuid();
  }

  protected void updateActivityStatus(String activityId, String appId, ExecutionStatus status) {
    activityService.updateStatus(activityId, appId, status);
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class JenkinsExecutionResponse implements DelegateTaskNotifyResponseData {
    private DelegateMetaInfo delegateMetaInfo;
    private ExecutionStatus executionStatus;
    private String jenkinsResult;
    private String errorMessage;
    private String jobUrl;
    private List<FilePathAssertionEntry> filePathAssertionMap = Lists.newArrayList();
    private String buildNumber;
    private Map<String, String> metadata;
    private Map<String, String> jobParameters;
    private Map<String, String> envVars;
    private String description;
    private String buildDisplayName;
    private String buildFullDisplayName;
    private String queuedBuildUrl;
    private JenkinsSubTaskType subTaskType;
    private String activityId;
    private Long timeElapsed; // time taken for task completion
  }
}
