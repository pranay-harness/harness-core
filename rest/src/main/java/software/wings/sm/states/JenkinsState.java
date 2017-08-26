package software.wings.sm.states;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static software.wings.api.JenkinsExecutionData.Builder.aJenkinsExecutionData;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.SettingAttribute.Category.CONNECTOR;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import software.wings.api.InstanceElement;
import software.wings.api.JenkinsExecutionData;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.common.Constants;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.impl.JenkinsSettingProvider;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
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
import software.wings.utils.XmlUtils;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by peeyushaggarwal on 10/21/16.
 */
public class JenkinsState extends State {
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
  @Attributes(title = "Wait interval before execution(in seconds)")
  public Integer getWaitInterval() {
    return super.getWaitInterval();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    ExecutionResponse response = executeInternal(context, activityId);
    return response;
  }

  /**
   * Execute internal execution response.
   *
   * @param context the context
   * @return the execution response
   */
  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    String jenkinsConfigExpression = null;
    String jobNameExpression = null;
    String accountId = ((ExecutionContextImpl) context).getApp().getAccountId();
    List<TemplateExpression> templateExpressions = getTemplateExpressions();
    if (templateExpressions != null && !templateExpressions.isEmpty()) {
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
      jenkinsConfig = (JenkinsConfig) context.getSettingValue(jenkinsConfigId, StateType.JENKINS.name());
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

    Map<String, String> jobParameterMap = CollectionUtils.isEmpty(jobParameters)
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
            .withParameters(new Object[] {jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(),
                jenkinsConfig.getPassword(), finalJobName, evaluatedParameters, evaluatedFilePathsForAssertion})
            .withEnvId(envId)
            .withInfrastructureMappingId(infrastructureMappingId)
            .build();

    if (getTimeoutMillis() != null) {
      delegateTask.setTimeout(getTimeoutMillis());
    }
    String delegateTaskId = delegateService.queueTask(delegateTask);

    JenkinsExecutionData jenkinsExecutionData =
        aJenkinsExecutionData().withJobName(finalJobName).withJobParameters(evaluatedParameters).build();
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
    return anExecutionResponse()
        .withExecutionStatus(jenkinsExecutionResponse.getExecutionStatus())
        .withStateExecutionData(jenkinsExecutionData)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Attributes(title = "Timeout (Milli-seconds)")
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
    if (CollectionUtils.isNotEmpty(jobParameters)) {
      jobParameters.forEach(parameterEntry -> patterns.add(parameterEntry.getValue()));
    }

    if (CollectionUtils.isNotEmpty(filePathsForAssertion)) {
      filePathsForAssertion.forEach(filePathAssertionEntry -> patterns.add(filePathAssertionEntry.getFilePath()));
    }
    return patterns;
  }

  protected String createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);

    Activity.Builder activityBuilder =
        anActivity()
            .withAppId(app.getUuid())
            .withApplicationName(app.getName())
            .withEnvironmentId(env.getUuid())
            .withEnvironmentName(env.getName())
            .withEnvironmentType(env.getEnvironmentType())
            .withCommandName(getName())
            .withType(Type.Verification)
            .withWorkflowType(executionContext.getWorkflowType())
            .withWorkflowExecutionName(executionContext.getWorkflowExecutionName())
            .withStateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
            .withStateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
            .withCommandType(getStateType())
            .withWorkflowExecutionId(executionContext.getWorkflowExecutionId());

    if (instanceElement != null) {
      activityBuilder.withServiceTemplateId(instanceElement.getServiceTemplateElement().getUuid())
          .withServiceTemplateName(instanceElement.getServiceTemplateElement().getName())
          .withServiceId(instanceElement.getServiceTemplateElement().getServiceElement().getUuid())
          .withServiceName(instanceElement.getServiceTemplateElement().getServiceElement().getName())
          .withServiceInstanceId(instanceElement.getUuid())
          .withHostName(instanceElement.getHost().getHostName());
    }

    return activityService.save(activityBuilder.build()).getUuid();
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

    public static final class Builder {
      private ExecutionStatus executionStatus;
      private String jenkinsResult;
      private String errorMessage;
      private String jobUrl;
      private List<FilePathAssertionEntry> filePathAssertionMap = Lists.newArrayList();

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

      public Builder but() {
        return aJenkinsExecutionResponse()
            .withExecutionStatus(executionStatus)
            .withJenkinsResult(jenkinsResult)
            .withErrorMessage(errorMessage)
            .withJobUrl(jobUrl)
            .withFilePathAssertionMap(filePathAssertionMap);
      }

      public JenkinsExecutionResponse build() {
        JenkinsExecutionResponse jenkinsExecutionResponse = new JenkinsExecutionResponse();
        jenkinsExecutionResponse.setExecutionStatus(executionStatus);
        jenkinsExecutionResponse.setJenkinsResult(jenkinsResult);
        jenkinsExecutionResponse.setErrorMessage(errorMessage);
        jenkinsExecutionResponse.setJobUrl(jobUrl);
        jenkinsExecutionResponse.setFilePathAssertionMap(filePathAssertionMap);
        return jenkinsExecutionResponse;
      }
    }
  }

  public static class ParameterEntry {
    @Attributes(title = "Parameter Name") String key;
    @Attributes(title = "Parameter Value") String value;

    /**
     * Getter for property 'filePath'.
     *
     * @return Value for property 'filePath'.
     */
    public String getKey() {
      return key;
    }

    /**
     * Setter for property 'filePath'.
     *
     * @param key Value to set for property 'filePath'.
     */
    public void setKey(String key) {
      this.key = key;
    }

    /**
     * Getter for property 'assertion'.
     *
     * @return Value for property 'assertion'.
     */
    public String getValue() {
      return value;
    }

    /**
     * Setter for property 'assertion'.
     *
     * @param value Value to set for property 'assertion'.
     */
    public void setValue(String value) {
      this.value = value;
    }
  }

  public static class FilePathAssertionEntry {
    @Attributes(title = "File/Artifact Path") String filePath;
    @Attributes(title = "Assertion") String assertion;
    @SchemaIgnore Status status;
    @SchemaIgnore String fileData;

    public FilePathAssertionEntry() {}

    public FilePathAssertionEntry(String filePath, String assertion, String fileData) {
      this.filePath = filePath;
      this.assertion = assertion;
      this.fileData = fileData;
    }

    public FilePathAssertionEntry(String filePath, String assertion, Status status) {
      this.filePath = filePath;
      this.assertion = assertion;
      this.status = status;
    }

    /**
     * Getter for property 'fileData'.
     *
     * @return Value for property 'fileData'.
     */
    public String getFileData() {
      return fileData;
    }

    /**
     * Setter for property 'fileData'.
     *
     * @param fileData Value to set for property 'fileData'.
     */
    public void setFileData(String fileData) {
      this.fileData = fileData;
    }

    /**
     * Getter for property 'filePath'.
     *
     * @return Value for property 'filePath'.
     */
    public String getFilePath() {
      return filePath;
    }

    /**
     * Setter for property 'filePath'.
     *
     * @param filePath Value to set for property 'filePath'.
     */
    public void setFilePath(String filePath) {
      this.filePath = filePath;
    }

    /**
     * Getter for property 'assertion'.
     *
     * @return Value for property 'assertion'.
     */
    public String getAssertion() {
      return assertion;
    }

    /**
     * Setter for property 'assertion'.
     *
     * @param assertion Value to set for property 'assertion'.
     */
    public void setAssertion(String assertion) {
      this.assertion = assertion;
    }

    /**
     * Getter for property 'status'.
     *
     * @return Value for property 'status'.
     */
    public Status getStatus() {
      return status;
    }

    /**
     * Setter for property 'status'.
     *
     * @param status Value to set for property 'status'.
     */
    public void setStatus(Status status) {
      this.status = status;
    }

    /**
     * Xml format.
     *
     * @return true, if successful
     */
    public boolean xmlFormat() {
      try {
        document();
        return true;
      } catch (Exception e) {
        return false;
      }
    }

    /**
     * Xpath.
     *
     * @param path the path
     * @return the string
     */
    public String xpath(String path) {
      try {
        return XmlUtils.xpath(document(), path);
      } catch (Exception e) {
        return null;
      }
    }

    private Document document() throws ParserConfigurationException, SAXException, IOException {
      return XmlUtils.parse(fileData);
    }

    public enum Status { NOT_FOUND, SUCCESS, FAILED }
  }
}
