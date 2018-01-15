package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.SettingAttribute.Category.CONNECTOR;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.InstanceElement;
import software.wings.api.JenkinsExecutionData;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.common.Constants;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.impl.JenkinsSettingProvider;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.utils.Validator;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
/**
 * Created by peeyushaggarwal on 10/21/16.
 */
public class JenkinsState extends State {
  public static final String COMMAND_UNIT_NAME = "Console Output";

  @Transient private static final Logger logger = LoggerFactory.getLogger(JenkinsState.class);

  @Inject private DelegateService delegateService;

  @EnumData(enumDataProvider = JenkinsSettingProvider.class)
  @Attributes(title = "Jenkins Server")
  private String jenkinsConfigId;

  @Attributes(title = "Job Name") private String jobName;

  @Attributes(title = "Job Parameters") private List<ParameterEntry> jobParameters = Lists.newArrayList();

  @Attributes(title = "Artifacts/Files Paths")
  private List<FilePathAssertionEntry> filePathsForAssertion = Lists.newArrayList();

  @Transient @Inject private JenkinsFactory jenkinsFactory;
  @Transient @Inject private SettingsService settingsService;
  @Transient @Inject private ExecutorService executorService;
  @Transient @Inject private ActivityService activityService;
  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;
  @Transient @Inject private SecretManager secretManager;

  @Transient @Inject private TemplateExpressionProcessor templateExpressionProcessor;

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

    String jenkinsConfigExpression = null;
    String jobNameExpression = null;
    String accountId = ((ExecutionContextImpl) context).getApp().getAccountId();
    List<TemplateExpression> templateExpressions = getTemplateExpressions();
    if (isNotEmpty(templateExpressions)) {
      for (TemplateExpression templateExpression : templateExpressions) {
        String fieldName = templateExpression.getFieldName();
        if (fieldName != null) {
          if (fieldName.equals("jenkinsConfigId")) {
            jenkinsConfigExpression = templateExpression.getExpression();
          } else if (fieldName.equals("jobName")) {
            jobNameExpression = templateExpression.getExpression();
          }
        }
      }
    }
    JenkinsConfig jenkinsConfig = null;
    if (jenkinsConfigExpression != null) {
      SettingAttribute settingAttribute =
          templateExpressionProcessor.resolveSettingAttribute(context, accountId, jenkinsConfigExpression, CONNECTOR);
      if (settingAttribute.getValue() instanceof JenkinsConfig) {
        jenkinsConfig = (JenkinsConfig) settingAttribute.getValue();
      }
    } else {
      jenkinsConfig =
          (JenkinsConfig) context.getGlobalSettingValue(accountId, jenkinsConfigId, StateType.JENKINS.name());
    }
    Validator.notNullCheck("JenkinsConfig", jenkinsConfig);

    String evaluatedJobName;
    try {
      if (jobNameExpression != null) {
        evaluatedJobName = context.renderExpression(jobNameExpression);
      } else {
        evaluatedJobName = context.renderExpression(jobName);
      }
    } catch (Exception e) {
      evaluatedJobName = jobName;
    }

    Map<String, String> jobParameterMap = isEmpty(jobParameters)
        ? Collections.emptyMap()
        : jobParameters.stream().collect(toMap(ParameterEntry::getKey, ParameterEntry::getValue));
    final String finalJobName = evaluatedJobName;

    Map<String, String> evaluatedParameters = Maps.newHashMap(jobParameterMap);
    evaluatedParameters.forEach((key, value) -> {
      String evaluatedValue;
      try {
        evaluatedValue = context.renderExpression(value);
      } catch (Exception e) {
        evaluatedValue = value;
      }
      evaluatedParameters.put(key, evaluatedValue);
    });

    Map<String, String> evaluatedFilePathsForAssertion = Maps.newHashMap();
    if (isNotEmpty(filePathsForAssertion)) {
      filePathsForAssertion.forEach(filePathAssertionMap -> {
        String evaluatedKey;
        try {
          evaluatedKey = context.renderExpression(filePathAssertionMap.getFilePath());
        } catch (Exception e) {
          evaluatedKey = filePathAssertionMap.getFilePath();
        }
        evaluatedFilePathsForAssertion.put(evaluatedKey, filePathAssertionMap.getAssertion());
      });
    }

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask =
        aDelegateTask()
            .withTaskType(getTaskType())
            .withAccountId(((ExecutionContextImpl) context).getApp().getAccountId())
            .withWaitId(activityId)
            .withAppId(((ExecutionContextImpl) context).getApp().getAppId())
            .withParameters(new Object[] {jenkinsConfig,
                secretManager.getEncryptionDetails(jenkinsConfig, context.getAppId(), context.getWorkflowExecutionId()),
                finalJobName, evaluatedParameters, evaluatedFilePathsForAssertion, activityId, COMMAND_UNIT_NAME})
            .withEnvId(envId)
            .withInfrastructureMappingId(infrastructureMappingId)
            .build();

    if (getTimeoutMillis() != null) {
      delegateTask.setTimeout(getTimeoutMillis());
    }
    String delegateTaskId = delegateService.queueTask(delegateTask);

    JenkinsExecutionData jenkinsExecutionData = JenkinsExecutionData.builder()
                                                    .jobName(finalJobName)
                                                    .jobParameters(evaluatedParameters)
                                                    .activityId(activityId)
                                                    .build();
    return anExecutionResponse()
        .withAsync(true)
        .withStateExecutionData(jenkinsExecutionData)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  protected TaskType getTaskType() {
    return TaskType.JENKINS;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    String activityId = response.keySet().iterator().next();
    JenkinsExecutionResponse jenkinsExecutionResponse = (JenkinsExecutionResponse) response.values().iterator().next();
    updateActivityStatus(
        activityId, ((ExecutionContextImpl) context).getApp().getUuid(), jenkinsExecutionResponse.getExecutionStatus());
    JenkinsExecutionData jenkinsExecutionData = (JenkinsExecutionData) context.getStateExecutionData();
    jenkinsExecutionData.setFilePathAssertionMap(jenkinsExecutionResponse.getFilePathAssertionMap());
    jenkinsExecutionData.setJobStatus(jenkinsExecutionResponse.getJenkinsResult());
    jenkinsExecutionData.setErrorMsg(jenkinsExecutionResponse.getErrorMessage());
    jenkinsExecutionData.setBuildUrl(jenkinsExecutionResponse.getJobUrl());
    jenkinsExecutionData.setBuildNumber(jenkinsExecutionResponse.getBuildNumber());
    jenkinsExecutionData.setMetadata(jenkinsExecutionResponse.getMetadata());
    return anExecutionResponse()
        .withExecutionStatus(jenkinsExecutionResponse.getExecutionStatus())
        .withStateExecutionData(jenkinsExecutionData)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + DEFAULT_ASYNC_CALL_TIMEOUT)
  @Override
  public Integer getTimeoutMillis() {
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
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);

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
                                          .serviceVariables(Maps.newHashMap())
                                          .status(ExecutionStatus.RUNNING)
                                          .commandUnitType(CommandUnitType.JENKINS);

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType().equals(BUILD)) {
      activityBuilder.environmentId(GLOBAL_ENV_ID).environmentName(GLOBAL_ENV_ID).environmentType(ALL);
    } else {
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

  public static final class JenkinsExecutionResponse implements NotifyResponseData {
    private ExecutionStatus executionStatus;
    private String jenkinsResult;
    private String errorMessage;
    private String jobUrl;
    private List<FilePathAssertionEntry> filePathAssertionMap = Lists.newArrayList();
    private String buildNumber;
    private Map<String, String> metadata;

    public JenkinsExecutionResponse() {}

    /**
     * Getter for property 'jobUrl'.
     *
     * @return Value for property 'jobUrl'.
     */
    public String getJobUrl() {
      return jobUrl;
    }

    /**
     * Setter for property 'jobUrl'.
     *
     * @param jobUrl Value to set for property 'jobUrl'.
     */
    public void setJobUrl(String jobUrl) {
      this.jobUrl = jobUrl;
    }

    /**
     * Getter for property 'executionStatus'.
     *
     * @return Value for property 'executionStatus'.
     */
    public ExecutionStatus getExecutionStatus() {
      return executionStatus;
    }

    /**
     * Setter for property 'executionStatus'.
     *
     * @param executionStatus Value to set for property 'executionStatus'.
     */
    public void setExecutionStatus(ExecutionStatus executionStatus) {
      this.executionStatus = executionStatus;
    }

    /**
     * Getter for property 'jenkinsResult'.
     *
     * @return Value for property 'jenkinsResult'.
     */
    public String getJenkinsResult() {
      return jenkinsResult;
    }

    /**
     * Setter for property 'jenkinsResult'.
     *
     * @param jenkinsResult Value to set for property 'jenkinsResult'.
     */
    public void setJenkinsResult(String jenkinsResult) {
      this.jenkinsResult = jenkinsResult;
    }

    /**
     * Getter for property 'errorMessage'.
     *
     * @return Value for property 'errorMessage'.
     */
    public String getErrorMessage() {
      return errorMessage;
    }

    /**
     * Setter for property 'errorMessage'.
     *
     * @param errorMessage Value to set for property 'errorMessage'.
     */
    public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
    }

    /**
     * Getter for property 'filePathAssertionMap'.
     *
     * @return Value for property 'filePathAssertionMap'.
     */
    public List<FilePathAssertionEntry> getFilePathAssertionMap() {
      return filePathAssertionMap;
    }

    /**
     * Setter for property 'filePathAssertionMap'.
     *
     * @param filePathAssertionMap Value to set for property 'filePathAssertionMap'.
     */
    public void setFilePathAssertionMap(List<FilePathAssertionEntry> filePathAssertionMap) {
      this.filePathAssertionMap = filePathAssertionMap;
    }

    public String getBuildNumber() {
      return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
      this.buildNumber = buildNumber;
    }

    public Map<String, String> getMetadata() {
      return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
      this.metadata = metadata;
    }

    public static final class Builder {
      private ExecutionStatus executionStatus;
      private String jenkinsResult;
      private String errorMessage;
      private String jobUrl;
      private List<FilePathAssertionEntry> filePathAssertionMap = Lists.newArrayList();
      private String buildNumber;
      private Map<String, String> metadata;

      private Builder() {}

      public static Builder aJenkinsExecutionResponse() {
        return new Builder();
      }

      public Builder withExecutionStatus(ExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
        return this;
      }

      public Builder withJenkinsResult(String jenkinsResult) {
        this.jenkinsResult = jenkinsResult;
        return this;
      }

      public Builder withErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
      }

      public Builder withJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
        return this;
      }

      public Builder withFilePathAssertionMap(List<FilePathAssertionEntry> filePathAssertionMap) {
        this.filePathAssertionMap = filePathAssertionMap;
        return this;
      }

      public Builder withBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
        return this;
      }

      public Builder withMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
      }

      public Builder but() {
        return aJenkinsExecutionResponse()
            .withExecutionStatus(executionStatus)
            .withJenkinsResult(jenkinsResult)
            .withErrorMessage(errorMessage)
            .withJobUrl(jobUrl)
            .withFilePathAssertionMap(filePathAssertionMap)
            .withBuildNumber(buildNumber)
            .withMetadata(metadata);
      }

      public JenkinsExecutionResponse build() {
        JenkinsExecutionResponse jenkinsExecutionResponse = new JenkinsExecutionResponse();
        jenkinsExecutionResponse.setExecutionStatus(executionStatus);
        jenkinsExecutionResponse.setJenkinsResult(jenkinsResult);
        jenkinsExecutionResponse.setErrorMessage(errorMessage);
        jenkinsExecutionResponse.setJobUrl(jobUrl);
        jenkinsExecutionResponse.setFilePathAssertionMap(filePathAssertionMap);
        jenkinsExecutionResponse.setBuildNumber(buildNumber);
        jenkinsExecutionResponse.setMetadata(metadata);
        return jenkinsExecutionResponse;
      }
    }
  }
}
