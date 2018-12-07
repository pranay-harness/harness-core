package software.wings.sm.states.collaboration;

import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.TaskType.JIRA;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
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
import software.wings.beans.SweepingOutput.Scope;
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
import software.wings.utils.KryoUtils;
import software.wings.utils.Validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.validation.constraints.NotNull;

public class JiraCreateUpdate extends State {
  private static final long JIRA_TASK_TIMEOUT_MILLIS = 60 * 1000;
  private static final String JIRA_SWEEPING_OUTPUT_NAME = "JIRA";
  private static final String JIRA_ISSUE_ID_KEY = "issueId";
  @Inject @Transient private ActivityService activityService;
  @Inject @Transient private transient WingsPersistence wingsPersistence;
  @Inject @Transient private DelegateServiceImpl delegateService;
  @Inject @Transient private transient SecretManager secretManager;
  @Inject @Transient private SweepingOutputService sweepingOutputService;

  @Attributes(title = "Request Type") @Getter @Setter @NotNull private JiraAction jiraAction;
  @Attributes(title = "JIRA Connector") @Getter @Setter @NotNull String jiraConnectorId;
  @Attributes(title = "Project") @Getter @Setter @NotNull private String project;
  @Attributes(title = "Issue Type") @Getter @Setter private String issueType;
  @Attributes(title = "Priority") @Getter @Setter private String priority;
  @Attributes(title = "Labels") @Getter @Setter private String labels;
  @Attributes(title = "Summary") @Getter @Setter private String summary;
  @Attributes(title = "Description") @Getter @Setter private String description;
  @Attributes(title = "Status") @Getter @Setter private String status;
  @Attributes(title = "Comment") @Getter @Setter private String comment;
  @Attributes(title = "Issue") @Getter @Setter private String issueId;

  public JiraCreateUpdate(String name) {
    super(name, StateType.JIRA_CREATE_UPDATE.getName());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    return executeInternal(context, activityId);
  }

  private ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;

    JiraConfig jiraConfig = getJiraConfig(jiraConnectorId);
    issueId = context.renderExpression(issueId);

    JiraTaskParameters parameters = JiraTaskParameters.builder()
                                        .jiraConfig(jiraConfig)
                                        .jiraAction(jiraAction)
                                        .issueId(issueId)
                                        .project(project)
                                        .issueType(issueType)
                                        .summary(summary)
                                        .status(status)
                                        .comment(comment)
                                        .encryptionDetails(secretManager.getEncryptionDetails(jiraConfig,
                                            executionContext.getAppId(), executionContext.getWorkflowExecutionId()))
                                        .accountId(((ExecutionContextImpl) context).getApp().getAccountId())
                                        .activityId(activityId)
                                        .appId(context.getAppId())
                                        .build();

    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(JIRA)
                                    .withAccountId(executionContext.getApp().getAccountId())
                                    .withWaitId(activityId)
                                    .withAppId(((ExecutionContextImpl) context).getApp().getAppId())
                                    .withParameters(new Object[] {parameters})
                                    .build();

    delegateTask.setTimeout(JIRA_TASK_TIMEOUT_MILLIS);
    String delegateTaskId = delegateService.queueTask(delegateTask);

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .withStateExecutionData(JiraExecutionData.builder().activityId(activityId).build())
        .build();
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
    Map<String, String> sweepingOutputMap = new HashMap<>();

    if (jiraExecutionData.getExecutionStatus() == ExecutionStatus.SUCCESS
        && jiraExecutionData.getJiraAction() == JiraAction.CREATE_TICKET) {
      sweepingOutputMap.put(JIRA_ISSUE_ID_KEY, jiraExecutionData.getIssueId());

      final SweepingOutput sweepingOutput = context.prepareSweepingOutputBuilder(Scope.PIPELINE)
                                                .name(JIRA_SWEEPING_OUTPUT_NAME)
                                                .output(KryoUtils.asDeflatedBytes(sweepingOutputMap))
                                                .build();

      sweepingOutputService.save(sweepingOutput);
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
