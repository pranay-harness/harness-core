package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.api.JiraExecutionData.JiraApprovalActionType.CREATE_WEBHOOK;
import static software.wings.api.JiraExecutionData.JiraApprovalActionType.WAIT_JIRA_APPROVAL;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.TaskType.JIRA;
import static software.wings.beans.alert.AlertType.ApprovalNeeded;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_NEEDED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_STATE_CHANGE_NOTIFICATION;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.JiraExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.JiraConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.beans.jira.JiraTaskParameters;
import software.wings.common.NotificationMessageResolver;
import software.wings.delegatetasks.jira.JiraAction;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
import software.wings.utils.Validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import javax.validation.constraints.NotNull;

public class JiraApprovalState extends AbstractApprovalState {
  private static final long JIRA_TASK_TIMEOUT_MILLIS = 60 * 1000;
  private static final String JIRA_WEBHOOK_URL_KEY = "webhookUrl";

  @Inject private transient WingsPersistence wingsPersistence;
  @Inject private ActivityService activityService;
  @Inject private DelegateService delegateService;
  @Inject private AlertService alertService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject private NotificationService notificationService;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private transient SecretManager secretManager;
  @Inject private JiraHelperService jiraHelperService;

  @Getter @Setter @NotNull String jiraConnectorId;
  @Getter @Setter private String issueType;
  @Getter @Setter private String priority;
  @Getter @Setter private String labels;
  @Getter @Setter private String assignee;
  @Getter @Setter private String description;
  @Getter @Setter private String approvalField;
  @Getter @Setter private String approvalValue;
  @Getter @Setter private boolean createJira;
  @Getter @Setter private String project;
  @Getter @Setter private String summary;
  @Getter @Setter private String status;
  @Getter @Setter private String issueId;

  public JiraApprovalState(String name) {
    super(name, StateType.JIRA_APPROVAL.getName());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    String activityId = createActivity(context);
    String approvalId = generateUuid();

    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;
    issueId = context.renderExpression(issueId);
    Application app = executionContext.getApp();
    JiraConfig jiraConfig = getJiraConfig(jiraConnectorId);
    JiraAction jiraAction;
    if (createJira) {
      jiraAction = JiraAction.CREATE_AND_APPROVE_TICKET;
    } else {
      jiraAction = JiraAction.APPROVE_TICKET;
    }

    String token = jiraHelperService.createJiraToken(
        app.getAppId(), context.getWorkflowExecutionId(), approvalId, approvalField, approvalValue);

    JiraTaskParameters parameters = JiraTaskParameters.builder()
                                        .jiraConfig(jiraConfig)
                                        .jiraAction(jiraAction)
                                        .project(project)
                                        .issueType(issueType)
                                        .summary(summary)
                                        .status(status)
                                        .encryptionDetails(secretManager.getEncryptionDetails(jiraConfig,
                                            executionContext.getAppId(), executionContext.getWorkflowExecutionId()))
                                        .accountId(((ExecutionContextImpl) context).getApp().getAccountId())
                                        .activityId(activityId)
                                        //                                        .issueKey(issueKey)
                                        .issueId(issueId)
                                        .approvalId(approvalId)
                                        .jiraToken(token)
                                        .appId(app.getAppId())
                                        .build();

    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(JIRA)
                                    .withAccountId(app.getAccountId())
                                    .withWaitId(activityId)
                                    .withAppId(app.getAppId())
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

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    Application app = ((ExecutionContextImpl) context).getApp();
    JiraExecutionData jiraExecutionData = (JiraExecutionData) response.values().iterator().next();

    if (jiraExecutionData.getJiraApprovalActionType() == CREATE_WEBHOOK) {
      String approvalId = jiraExecutionData.getApprovalId();
      ApprovalNeededAlert approvalNeededAlert = ApprovalNeededAlert.builder()
                                                    .executionId(context.getWorkflowExecutionId())
                                                    .approvalId(approvalId)
                                                    .name(context.getWorkflowExecutionName())
                                                    .build();
      populateApprovalAlert(approvalNeededAlert, context);
      alertService.openAlert(app.getAccountId(), app.getUuid(), ApprovalNeeded, approvalNeededAlert);

      Map<String, String> placeholderValues = getPlaceholderValues(context, "", PAUSED);
      sendApprovalNotification(app.getAccountId(), APPROVAL_NEEDED_NOTIFICATION, placeholderValues);

      return anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(Collections.singletonList(approvalId))
          .withExecutionStatus(PAUSED)
          .withErrorMessage(jiraExecutionData.getErrorMessage())
          .withStateExecutionData(jiraExecutionData)
          .build();

    } else if (jiraExecutionData.getJiraApprovalActionType() == WAIT_JIRA_APPROVAL) {
      // Close the alert

      ApprovalNeededAlert approvalNeededAlert = ApprovalNeededAlert.builder()
                                                    .executionId(context.getWorkflowExecutionId())
                                                    .approvalId(jiraExecutionData.getApprovalId())
                                                    .build();
      populateApprovalAlert(approvalNeededAlert, context);
      alertService.closeAlert(app.getAccountId(), app.getUuid(), ApprovalNeeded, approvalNeededAlert);

      Map<String, String> placeholderValues =
          getPlaceholderValues(context, jiraExecutionData.getApprovedBy().getName(), jiraExecutionData.getStatus());
      sendApprovalNotification(app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues);

      // delete webhook created
      JiraConfig jiraConfig = getJiraConfig(jiraConnectorId);

      JiraAction jiraAction = JiraAction.DELETE_WEBHOOK;
      String activityId = createActivity(context);
      JiraTaskParameters parameters = JiraTaskParameters.builder()
                                          .jiraConfig(jiraConfig)
                                          .jiraAction(jiraAction)
                                          .webhookUrl(jiraExecutionData.getWebhookUrl())
                                          .encryptionDetails(secretManager.getEncryptionDetails(
                                              jiraConfig, app.getAppId(), context.getWorkflowExecutionId()))
                                          .activityId(activityId)
                                          .appId(app.getAppId())
                                          .accountId(app.getAccountId())
                                          .build();

      DelegateTask delegateTask = aDelegateTask()
                                      .withTaskType(JIRA)
                                      .withAccountId(app.getAccountId())
                                      .withWaitId(activityId)
                                      .withAppId(((ExecutionContextImpl) context).getApp().getAppId())
                                      .withParameters(new Object[] {parameters})
                                      .build();
      delegateTask.setTimeout(JIRA_TASK_TIMEOUT_MILLIS);
      String delegateTaskId = delegateService.queueTask(delegateTask);

      return anExecutionResponse()
          .withAsync(true)
          .withStateExecutionData(jiraExecutionData)
          .withCorrelationIds(Collections.singletonList(activityId))
          .withExecutionStatus(jiraExecutionData.getStatus())
          .withDelegateTaskId(delegateTaskId)
          .withErrorMessage(jiraExecutionData.getErrorMessage())
          .build();
    } else {
      return anExecutionResponse()
          .withStateExecutionData(jiraExecutionData)
          .withExecutionStatus(jiraExecutionData.getExecutionStatus())
          .withErrorMessage(jiraExecutionData.getErrorMessage())
          .build();
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    handleAbortEventAbstract(context);
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
}
