package software.wings.sm.states.collaboration;

import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.TaskType.JIRA;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import lombok.Getter;
import lombok.Setter;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.JiraExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.JiraConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SweepingOutput;
import software.wings.beans.jira.JiraTaskParameters;
import software.wings.delegatetasks.jira.JiraAction;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.states.mixin.SweepingOutputStateMixin;
import software.wings.utils.Validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.validation.constraints.NotNull;

public class JiraCreateUpdate extends State implements SweepingOutputStateMixin {
  private static final long JIRA_TASK_TIMEOUT_MILLIS = 60 * 1000;
  private static final String JIRA_ISSUE_ID = "issueId";
  private static final String JIRA_ISSUE_KEY = "issueKey";

  @Inject private transient ActivityService activityService;
  @Inject @Transient private transient WingsPersistence wingsPersistence;
  @Inject @Transient private DelegateServiceImpl delegateService;
  @Inject @Transient private transient SecretManager secretManager;
  @Inject @Transient private SweepingOutputService sweepingOutputService;

  @Getter @Setter @NotNull private JiraAction jiraAction;
  @Getter @Setter @NotNull String jiraConnectorId;
  @Getter @Setter @NotNull private String project;
  @Getter @Setter private String issueType;
  @Getter @Setter private String priority;
  @Getter @Setter private List<String> labels;
  @Getter @Setter private String summary;
  @Getter @Setter private String description;
  @Getter @Setter private String status;
  @Getter @Setter private String comment;
  @Getter @Setter private String issueId;
  private Map<String, Object> customFields;

  public Map<String, Object> fetchCustomFields() {
    return customFields;
  }

  public void setCustomFields(Map<String, Object> customFields) {
    this.customFields = customFields;
  }

  @Getter @Setter private SweepingOutput.Scope sweepingOutputScope;
  @Getter @Setter private String sweepingOutputName;

  public JiraCreateUpdate(String name) {
    super(name, StateType.JIRA_CREATE_UPDATE.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    return executeInternal(context, activityId);
  }

  private ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;

    JiraConfig jiraConfig = getJiraConfig(jiraConnectorId);
    renderExpressions(context);

    JiraTaskParameters parameters = JiraTaskParameters.builder()
                                        .jiraConfig(jiraConfig)
                                        .jiraAction(jiraAction)
                                        .issueId(issueId)
                                        .project(project)
                                        .issueType(issueType)
                                        .summary(summary)
                                        .status(status)
                                        .description(description)
                                        .labels(labels)
                                        .customFields(customFields)
                                        .comment(comment)
                                        .priority(priority)
                                        .encryptionDetails(secretManager.getEncryptionDetails(jiraConfig,
                                            executionContext.getAppId(), executionContext.getWorkflowExecutionId()))
                                        .accountId(((ExecutionContextImpl) context).getApp().getAccountId())
                                        .activityId(activityId)
                                        .appId(context.getAppId())
                                        .build();

    DelegateTask delegateTask =
        DelegateTask.builder()
            .async(true)
            .taskType(JIRA.name())
            .accountId(executionContext.getApp().getAccountId())
            .waitId(activityId)
            .appId(((ExecutionContextImpl) context).getApp().getAppId())
            .data(TaskData.builder().parameters(new Object[] {parameters}).timeout(JIRA_TASK_TIMEOUT_MILLIS).build())
            .build();
    String delegateTaskId = delegateService.queueTask(delegateTask);

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .withStateExecutionData(JiraExecutionData.builder().activityId(activityId).build())
        .build();
  }

  private void renderExpressions(ExecutionContext context) {
    issueId = context.renderExpression(issueId);
    labels = context.renderExpressionList(labels);
    summary = context.renderExpression(summary);
    description = context.renderExpression(description);
    comment = context.renderExpression(comment);
  }

  private JiraConfig getJiraConfig(String jiraConnectorId) {
    SettingAttribute jiraSettingAttribute = wingsPersistence.get(SettingAttribute.class, jiraConnectorId);
    Validator.notNullCheck("jiraSettingAttribute", jiraSettingAttribute);

    if (!(jiraSettingAttribute.getValue() instanceof JiraConfig)) {
      throw new InvalidRequestException("Type of Setting Attribute Value is not JiraConfig");
    }

    return (JiraConfig) jiraSettingAttribute.getValue();
  }

  private String createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .commandName(getName())
                                          .type(Type.Command)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(Arrays.asList())
                                          .status(ExecutionStatus.RUNNING);

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType().equals(BUILD)) {
      activityBuilder.environmentId(GLOBAL_ENV_ID).environmentName(GLOBAL_ENV_ID).environmentType(ALL);
    } else {
      activityBuilder.environmentId(env.getUuid())
          .environmentName(env.getName())
          .environmentType(env.getEnvironmentType());
    }

    Activity activity = activityBuilder.build();
    activity.setAppId(app.getUuid());

    return activityService.save(activity).getUuid();
  }

  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    Entry<String, ResponseData> responseEntry = response.entrySet().iterator().next();
    String activityId = responseEntry.getKey();

    JiraExecutionData jiraExecutionData = (JiraExecutionData) responseEntry.getValue();
    jiraExecutionData.setActivityId(activityId);

    if (jiraExecutionData.getExecutionStatus() == ExecutionStatus.SUCCESS) {
      Map<String, String> sweepingOutputMap = new HashMap<>();
      sweepingOutputMap.put(JIRA_ISSUE_ID, jiraExecutionData.getIssueId());
      sweepingOutputMap.put(JIRA_ISSUE_KEY, jiraExecutionData.getIssueKey());
      handleSweepingOutput(sweepingOutputService, context, sweepingOutputMap);
    }

    return anExecutionResponse()
        .withStateExecutionData(jiraExecutionData)
        .withExecutionStatus(jiraExecutionData.getExecutionStatus())
        .withErrorMessage(jiraExecutionData.getErrorMessage())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
