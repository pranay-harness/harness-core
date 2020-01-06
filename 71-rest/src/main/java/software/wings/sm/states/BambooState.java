package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.sm.StateType.BAMBOO;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.BambooExecutionData;
import software.wings.api.InstanceElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Application;
import software.wings.beans.BambooConfig;
import software.wings.beans.Environment;
import software.wings.beans.TaskType;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 8/28/17.
 */
@Slf4j
public class BambooState extends State {
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
      return ExecutionResponse.builder()
          .executionStatus(FAILED)
          .errorMessage("Bamboo Server was deleted. Please update with an appropriate server")
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
    String infrastructureMappingId = context.fetchInfraMappingId();

    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(((ExecutionContextImpl) context).getApp().getAccountId())
                                    .waitId(activityId)
                                    .appId(((ExecutionContextImpl) context).getApp().getAppId())
                                    .data(TaskData.builder()
                                              .taskType(getTaskType().name())
                                              .parameters(new Object[] {bambooConfig,
                                                  secretManager.getEncryptionDetails(bambooConfig, context.getAppId(),
                                                      context.getWorkflowExecutionId()),
                                                  finalPlanName, evaluatedParameters, evaluatedFilePathsForAssertion})
                                              .timeout(defaultIfNullTimeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                                              .build())
                                    .envId(envId)
                                    .infrastructureMappingId(infrastructureMappingId)
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);
    return ExecutionResponse.builder()
        .async(true)
        .stateExecutionData(BambooExecutionData.builder()
                                .planName(planName)
                                .parameters(evaluatedParameters)
                                .filePathAssertionEntries(evaluatedFilePathsForAssertion)
                                .build())
        .correlationIds(Collections.singletonList(activityId))
        .delegateTaskId(delegateTaskId)
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

    return ExecutionResponse.builder()
        .executionStatus(bambooExecutionResponse.getExecutionStatus())
        .stateExecutionData(bambooExecutionData)
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
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

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
                                          .status(RUNNING)
                                          .triggeredBy(TriggeredBy.builder()
                                                           .email(workflowStandardParams.getCurrentUser().getEmail())
                                                           .name(workflowStandardParams.getCurrentUser().getName())
                                                           .build());

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType() == BUILD) {
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

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class BambooExecutionResponse implements DelegateTaskNotifyResponseData {
    private DelegateMetaInfo delegateMetaInfo;
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
