package software.wings.sm.states;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static software.wings.api.BambooExecutionData.Builder.aBambooExecutionData;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.BambooExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.common.Constants;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.service.impl.BambooSettingProvider;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 8/28/17.
 */
public class BambooState extends State {
  @Transient private static final Logger logger = LoggerFactory.getLogger(BambooState.class);

  @EnumData(enumDataProvider = BambooSettingProvider.class)
  @Attributes(title = "Bamboo Server")
  private String bambooConfigId;

  @Attributes(title = "Plan Name") private String planName;

  @Attributes(title = "Parameters") private List<ParameterEntry> parameters = Lists.newArrayList();

  @SchemaIgnore
  @Attributes(title = "Artifacts/Files Paths")
  private List<FilePathAssertionEntry> filePathsForAssertion = Lists.newArrayList();

  @Transient @Inject private TemplateExpressionProcessor templateExpressionProcessor;
  @Transient @Inject private ActivityService activityService;
  @Inject private DelegateService delegateService;

  public BambooState(String name) {
    super(name, StateType.BAMBOO.name());
  }

  public String getBambooConfigId() {
    return bambooConfigId;
  }

  public void setBambooConfigId(String bambooConfigId) {
    this.bambooConfigId = bambooConfigId;
  }

  public String getPlanName() {
    return planName;
  }

  public void setPlanName(String planName) {
    this.planName = planName;
  }

  public List<ParameterEntry> getParameters() {
    return parameters;
  }

  public void setParameters(List<ParameterEntry> parameters) {
    this.parameters = parameters;
  }

  @SchemaIgnore
  public List<FilePathAssertionEntry> getFilePathsForAssertion() {
    return filePathsForAssertion;
  }

  public void setFilePathsForAssertion(List<FilePathAssertionEntry> filePathsForAssertion) {
    this.filePathsForAssertion = filePathsForAssertion;
  }

  @Override
  @Attributes(title = "Wait interval before execution(in seconds)")
  public Integer getWaitInterval() {
    return super.getWaitInterval();
  }

  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + DEFAULT_ASYNC_CALL_TIMEOUT)
  @Override
  public Integer getTimeoutMillis() {
    return super.getTimeoutMillis();
  }

  protected TaskType getTaskType() {
    return TaskType.BAMBOO;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    ExecutionResponse response = executeInternal(context, activityId);
    return response;
  }

  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    String bambooConfigExpression = null;
    String planNameExpression = null;
    String accountId = ((ExecutionContextImpl) context).getApp().getAccountId();
    List<TemplateExpression> templateExpressions = getTemplateExpressions();
    if (templateExpressions != null && !templateExpressions.isEmpty()) {
      for (TemplateExpression templateExpression : templateExpressions) {
        String fieldName = templateExpression.getFieldName();
        if (fieldName != null) {
          if (fieldName.equals("bambooConfigId")) {
            bambooConfigExpression = templateExpression.getExpression();
          } else if (fieldName.equals("planName")) {
            planNameExpression = templateExpression.getExpression();
          }
        }
      }
    }
    BambooConfig bambooConfig = null;
    if (bambooConfigExpression != null) {
      SettingAttribute settingAttribute = templateExpressionProcessor.resolveSettingAttribute(
          context, accountId, bambooConfigExpression, SettingAttribute.Category.CONNECTOR);
      if (settingAttribute.getValue() instanceof BambooConfig) {
        bambooConfig = (BambooConfig) settingAttribute.getValue();
      }
    } else {
      bambooConfig = (BambooConfig) context.getSettingValue(bambooConfigId, StateType.BAMBOO.name());
    }
    Validator.notNullCheck("BambooConfig", bambooConfig);

    String evaluatedPlanName;
    try {
      if (planNameExpression != null) {
        evaluatedPlanName = context.renderExpression(planNameExpression);
      } else {
        evaluatedPlanName = context.renderExpression(planName);
      }
    } catch (Exception e) {
      evaluatedPlanName = planName;
    }
    List<ParameterEntry> evaluatedParameters = new ArrayList<>();
    if (isNotEmpty(parameters)) {
      parameters.forEach(parameterEntry -> {
        String evaluatedValue;
        try {
          evaluatedValue = context.renderExpression(parameterEntry.getValue());
        } catch (Exception e) {
          evaluatedValue = parameterEntry.value;
        }
        ParameterEntry evaluatedParameterEntry = new ParameterEntry();
        evaluatedParameterEntry.setKey(parameterEntry.getKey());
        evaluatedParameterEntry.setValue(evaluatedValue);
        evaluatedParameters.add(evaluatedParameterEntry);
      });
    }
    /* evaluatedParameters.forEach((key, value) -> {
       String evaluatedValue;
       try {
         evaluatedValue = context.renderExpression(value);
       } catch (Exception e) {
         evaluatedValue = value;
       }
       evaluatedParameters.put(key, evaluatedValue);
     });*/

    List<FilePathAssertionEntry> evaluatedFilePathsForAssertion = new ArrayList<>();
    if (isNotEmpty(filePathsForAssertion)) {
      filePathsForAssertion.forEach(filePathAssertionEntry -> {
        String evaluatedFilePath = filePathAssertionEntry.getFilePath();
        try {
          evaluatedFilePath = context.renderExpression(evaluatedFilePath);

        } catch (Exception e) {
          evaluatedFilePath = filePathAssertionEntry.getFilePath();
        }
        FilePathAssertionEntry evaluatedPathAssertionEntry = new FilePathAssertionEntry();
        evaluatedPathAssertionEntry.setFilePath(evaluatedFilePath);
        evaluatedPathAssertionEntry.setAssertion(filePathAssertionEntry.getAssertion());
        evaluatedFilePathsForAssertion.add(filePathAssertionEntry);
      });
    }

    final String finalPlanName = evaluatedPlanName;
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask =
        DelegateTask.Builder.aDelegateTask()
            .withTaskType(getTaskType())
            .withAccountId(((ExecutionContextImpl) context).getApp().getAccountId())
            .withWaitId(activityId)
            .withAppId(((ExecutionContextImpl) context).getApp().getAppId())
            .withParameters(new Object[] {bambooConfig.getBambooUrl(), bambooConfig.getUsername(),
                bambooConfig.getPassword(), finalPlanName, evaluatedParameters, evaluatedFilePathsForAssertion})
            .withEnvId(envId)
            .withInfrastructureMappingId(infrastructureMappingId)
            .build();

    if (getTimeoutMillis() != null) {
      delegateTask.setTimeout(getTimeoutMillis());
    }
    logger.info("Sending Bamboo task to delegate");
    String delegateTaskId = delegateService.queueTask(delegateTask);
    logger.info("Delegate task id {}", delegateTaskId);
    BambooExecutionData bambooExecutionData = aBambooExecutionData()
                                                  .withPlanName(planName)
                                                  .withParameters(evaluatedParameters)
                                                  .withFilePathAssertionMap(evaluatedFilePathsForAssertion)
                                                  .build();
    return anExecutionResponse()
        .withAsync(true)
        .withStateExecutionData(bambooExecutionData)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    String activityId = response.keySet().iterator().next();
    BambooExecutionResponse bambooExecutionResponse = (BambooExecutionResponse) response.values().iterator().next();
    updateActivityStatus(
        activityId, ((ExecutionContextImpl) context).getApp().getUuid(), bambooExecutionResponse.getExecutionStatus());
    BambooExecutionData bambooExecutionData = (BambooExecutionData) context.getStateExecutionData();
    bambooExecutionData.setProjectName(bambooExecutionResponse.getProjectName());
    bambooExecutionData.setPlanName(bambooExecutionResponse.getPlanName());
    bambooExecutionData.setBuildNumber(bambooExecutionResponse.getBuildNumber());
    bambooExecutionData.setBuildStatus(bambooExecutionResponse.getBuildStatus());
    bambooExecutionData.setBuildUrl(bambooExecutionResponse.getBuildUrl());
    bambooExecutionData.setErrorMsg(bambooExecutionResponse.getErrorMessage());
    bambooExecutionData.setParameters(bambooExecutionResponse.getParameters());
    bambooExecutionData.setFilePathAssertionEntries(bambooExecutionResponse.getFilePathAssertionMap());
    return anExecutionResponse()
        .withExecutionStatus(bambooExecutionResponse.getExecutionStatus())
        .withStateExecutionData(bambooExecutionData)
        .build();
  }

  @Override
  @SchemaIgnore
  public List<String> getPatternsForRequiredContextElementType() {
    List<String> patterns = new ArrayList<>();
    patterns.add(planName);
    if (CollectionUtils.isNotEmpty(parameters)) {
      parameters.forEach(parameterEntry -> patterns.add(parameterEntry.getValue()));
    }

    if (CollectionUtils.isNotEmpty(filePathsForAssertion)) {
      filePathsForAssertion.forEach(filePathAssertionEntry -> patterns.add(filePathAssertionEntry.getFilePath()));
    }
    return patterns;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // TODO: Handle the abort event
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
            .withType(Activity.Type.Verification)
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

  public static final class BambooExecutionResponse implements NotifyResponseData {
    private String projectName;
    private String planName;
    private String planUrl;
    private String buildStatus;
    private String buildUrl;
    private String buildNumber;
    private ExecutionStatus executionStatus;
    private String errorMessage;
    private List<ParameterEntry> parameters;
    private List<FilePathAssertionEntry> filePathAssertionMap;

    public BambooExecutionResponse() {}

    public String getBuildNumber() {
      return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
      this.buildNumber = buildNumber;
    }

    public String getProjectName() {
      return projectName;
    }

    public void setProjectName(String projectName) {
      this.projectName = projectName;
    }

    public String getPlanName() {
      return planName;
    }

    public void setPlanName(String planName) {
      this.planName = planName;
    }

    public String getPlanUrl() {
      return planUrl;
    }

    public void setPlanUrl(String planUrl) {
      this.planUrl = planUrl;
    }

    public List<ParameterEntry> getParameters() {
      return parameters;
    }

    public void setParameters(List<ParameterEntry> parameters) {
      this.parameters = parameters;
    }

    /**
     * Getter for property 'planUrl'.
     *
     * @return Value for property 'planUrl'.
     */
    public String getBuildUrl() {
      return buildUrl;
    }

    /**
     * Setter for property 'buildUrl'.
     *
     * @param buildUrl Value to set for property 'buildUrl'.
     */
    public void setBuildUrl(String buildUrl) {
      this.buildUrl = buildUrl;
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
     * Getter for property 'BambooResult'.
     *
     * @return Value for property 'BambooResult'.
     */
    public String getBuildStatus() {
      return buildStatus;
    }

    /**
     * Setter for property 'buildStatus'.
     *
     * @param buildStatus Value to set for property 'buildStatus'.
     */
    public void setBuildStatus(String buildStatus) {
      this.buildStatus = buildStatus;
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
      private String projectName;
      private String planName;
      private String planUrl;
      private String buildStatus;
      private String buildUrl;
      private String buildNumber;
      private ExecutionStatus executionStatus;
      private String errorMessage;
      private List<ParameterEntry> parameters;
      private List<FilePathAssertionEntry> filePathAssertionMap = Lists.newArrayList();

      private Builder() {}

      public static Builder aBambooExecutionResponse() {
        return new Builder();
      }
      public Builder withProjectName(String projectName) {
        this.projectName = projectName;
        return this;
      }
      public Builder withPlanName(String planName) {
        this.planName = planName;
        return this;
      }

      public Builder withPlanUrl(String planUrl) {
        this.planUrl = planUrl;
        return this;
      }

      public Builder withBuildStatus(String buildStatus) {
        this.buildStatus = buildStatus;
        return this;
      }

      public Builder withExecutionStatus(ExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
        return this;
      }

      public Builder withErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
      }

      public Builder withBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
        return this;
      }

      public Builder withBuildNumber(String buildUrl) {
        this.buildUrl = buildUrl;
        return this;
      }

      public Builder but() {
        return aBambooExecutionResponse()
            .withProjectName(projectName)
            .withPlanName(planName)
            .withPlanUrl(planUrl)
            .withExecutionStatus(executionStatus)
            .withErrorMessage(errorMessage)
            .withBuildUrl(buildUrl)
            .withBuildStatus(buildStatus)
            .withBuildNumber(buildNumber);
      }

      public BambooExecutionResponse build() {
        BambooExecutionResponse bambooExecutionResponse = new BambooExecutionResponse();
        bambooExecutionResponse.setProjectName(projectName);
        bambooExecutionResponse.setPlanName(planName);
        bambooExecutionResponse.setBuildUrl(buildUrl);
        bambooExecutionResponse.setBuildNumber(buildNumber);
        bambooExecutionResponse.setBuildStatus(buildStatus);
        bambooExecutionResponse.setParameters(parameters);
        bambooExecutionResponse.setExecutionStatus(executionStatus);
        bambooExecutionResponse.setErrorMessage(errorMessage);
        bambooExecutionResponse.setFilePathAssertionMap(filePathAssertionMap);
        return bambooExecutionResponse;
      }
    }
  }
}
