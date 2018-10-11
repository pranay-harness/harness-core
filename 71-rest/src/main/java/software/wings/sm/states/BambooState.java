package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.StateType.BAMBOO;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.task.protocol.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.BambooExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Application;
import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.TaskType;
import software.wings.common.Constants;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 8/28/17.
 */
public class BambooState extends State {
  @Transient private static final Logger logger = LoggerFactory.getLogger(BambooState.class);

  @Attributes(title = "Bamboo Server") private String bambooConfigId;

  @Attributes(title = "Plan Name") private String planName;

  @Attributes(title = "Parameters") private List<ParameterEntry> parameters = Lists.newArrayList();

  @SchemaIgnore
  @Attributes(title = "Artifacts/Files Paths")
  private List<FilePathAssertionEntry> filePathsForAssertion = Lists.newArrayList();

  @Transient @Inject private ActivityService activityService;

  @Inject private DelegateService delegateService;
  @Inject private SecretManager secretManager;

  public BambooState(String name) {
    super(name, BAMBOO.name());
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
  @Attributes(title = "Wait interval before execution (s)")
  public Integer getWaitInterval() {
    return super.getWaitInterval();
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

  protected TaskType getTaskType() {
    return TaskType.BAMBOO;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    return executeInternal(context, activityId);
  }

  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = (workflowStandardParams == null || workflowStandardParams.getEnv() == null)
        ? null
        : workflowStandardParams.getEnv().getUuid();
    String accountId = ((ExecutionContextImpl) context).getApp().getAccountId();

    BambooConfig bambooConfig = (BambooConfig) context.getGlobalSettingValue(accountId, bambooConfigId);
    if (bambooConfig == null) {
      logger.warn("BamboodConfig Id {} does not exist. It might have been deleted", bambooConfigId);
      return anExecutionResponse()
          .withExecutionStatus(FAILED)
          .withErrorMessage("Bamboo Server was deleted. Please update with an appropriate server")
          .build();
    }

    String evaluatedPlanName = context.renderExpression(planName);

    List<ParameterEntry> evaluatedParameters = new ArrayList<>();
    if (isNotEmpty(parameters)) {
      parameters.forEach(parameterEntry -> {
        ParameterEntry evaluatedParameterEntry = new ParameterEntry();
        evaluatedParameterEntry.setKey(parameterEntry.getKey());
        evaluatedParameterEntry.setValue(context.renderExpression(parameterEntry.getValue()));
        evaluatedParameters.add(evaluatedParameterEntry);
      });
    }

    List<FilePathAssertionEntry> evaluatedFilePathsForAssertion = new ArrayList<>();
    if (isNotEmpty(filePathsForAssertion)) {
      filePathsForAssertion.forEach(filePathAssertionEntry -> {
        FilePathAssertionEntry evaluatedPathAssertionEntry = new FilePathAssertionEntry();
        evaluatedPathAssertionEntry.setFilePath(context.renderExpression(filePathAssertionEntry.getFilePath()));
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
            .withParameters(new Object[] {bambooConfig,
                secretManager.getEncryptionDetails(bambooConfig, context.getAppId(), context.getWorkflowExecutionId()),
                finalPlanName, evaluatedParameters, evaluatedFilePathsForAssertion})
            .withEnvId(envId)
            .withInfrastructureMappingId(infrastructureMappingId)
            .build();

    if (getTimeoutMillis() != null) {
      delegateTask.setTimeout(getTimeoutMillis());
    }
    String delegateTaskId = delegateService.queueTask(delegateTask);
    return anExecutionResponse()
        .withAsync(true)
        .withStateExecutionData(BambooExecutionData.builder()
                                    .planName(planName)
                                    .parameters(evaluatedParameters)
                                    .filePathAssertionEntries(evaluatedFilePathsForAssertion)
                                    .build())
        .withCorrelationIds(Collections.singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
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
    bambooExecutionData.setDelegateMetaInfo(bambooExecutionResponse.getDelegateMetaInfo());

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
    if (isNotEmpty(parameters)) {
      parameters.forEach(parameterEntry -> patterns.add(parameterEntry.getValue()));
    }

    if (isNotEmpty(filePathsForAssertion)) {
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

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .commandName(getName())
                                          .type(Activity.Type.Verification)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(Collections.emptyList())
                                          .status(RUNNING);

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

  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class BambooExecutionResponse extends DelegateTaskNotifyResponseData {
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
  }
}
