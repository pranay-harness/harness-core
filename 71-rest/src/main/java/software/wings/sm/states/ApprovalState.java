package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.ABORTED;
import static io.harness.beans.ExecutionStatus.EXPIRED;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.REJECTED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.event.model.EventConstants.ENVIRONMENT_ID;
import static io.harness.event.model.EventConstants.ENVIRONMENT_NAME;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.alert.AlertType.ApprovalNeeded;
import static software.wings.common.Constants.DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS;
import static software.wings.common.Constants.SCRIPT_APPROVAL_COMMAND;
import static software.wings.common.Constants.SCRIPT_APPROVAL_JOB_GROUP;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_EXPIRED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_NEEDED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_STATE_CHANGE_NOTIFICATION;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.states.ApprovalState.ApprovalStateType.USER_GROUP;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.scheduler.PersistentScheduler;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.JiraExecutionData;
import software.wings.api.ShellScriptApprovalExecutionData;
import software.wings.api.WorkflowElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.beans.approval.ApprovalStateParams;
import software.wings.beans.approval.JiraApprovalParams;
import software.wings.beans.approval.ServiceNowApprovalParams;
import software.wings.beans.approval.ShellScriptApprovalParams;
import software.wings.beans.command.Command.Builder;
import software.wings.beans.command.CommandType;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.scheduler.JiraPollingJob;
import software.wings.scheduler.ScriptApprovalJob;
import software.wings.scheduler.ServiceNowApprovalJob;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.servicenow.ServiceNowService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApprovalState extends State {
  @Getter @Setter private String groupName;
  @Getter @Setter private List<String> userGroups = new ArrayList<>();
  @Getter @Setter private boolean disable;

  public enum ApprovalStateType { JIRA, USER_GROUP, SHELL_SCRIPT, SERVICENOW }

  @Inject private AlertService alertService;
  @Inject private NotificationService notificationService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject private NotificationDispatcherService notificationDispatcherService;
  @Inject private EmailNotificationService emailNotificationService;
  @Inject private UserGroupService userGroupService;
  @Inject private UserService userService;
  @Inject private JiraHelperService jiraHelperService;
  @Inject private ServiceNowService serviceNowService;
  @Inject private transient ActivityService activityService;
  @Inject @Named("ServiceJobScheduler") private PersistentScheduler serviceJobScheduler;

  @Getter @Setter ApprovalStateParams approvalStateParams;
  @Getter @Setter ApprovalStateType approvalStateType = USER_GROUP;

  public ApprovalState(String name) {
    super(name, StateType.APPROVAL.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String approvalId = generateUuid();
    ApprovalStateExecutionData executionData = ApprovalStateExecutionData.builder()
                                                   .approvalId(approvalId)
                                                   .approvalStateType(approvalStateType)
                                                   .timeoutMillis(getTimeoutMillis())
                                                   .build();

    if (disable) {
      return anExecutionResponse()
          .withExecutionStatus(SKIPPED)
          .withErrorMessage("Approval step is disabled. Approval is skipped.")
          .withStateExecutionData(executionData)
          .build();
    }

    setPipelineVariables(context);

    Application app = ((ExecutionContextImpl) context).getApp();
    Map<String, String> placeholderValues = getPlaceholderValues(context, "", PAUSED);
    // Open an alert
    ApprovalNeededAlert approvalNeededAlert = ApprovalNeededAlert.builder()
                                                  .executionId(context.getWorkflowExecutionId())
                                                  .approvalId(approvalId)
                                                  .name(context.getWorkflowExecutionName())
                                                  .build();
    populateApprovalAlert(approvalNeededAlert, context);
    alertService.openAlert(app.getAccountId(), app.getUuid(), ApprovalNeeded, approvalNeededAlert);
    sendApprovalNotification(app.getAccountId(), APPROVAL_NEEDED_NOTIFICATION, placeholderValues);
    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetailsWithoutGraph(app.getAppId(), context.getWorkflowExecutionId());
    if (workflowExecution != null) {
      if (workflowExecution.getPipelineSummary() != null) {
        executionData.setWorkflowId(workflowExecution.getPipelineSummary().getPipelineId());
      } else {
        executionData.setWorkflowId(workflowExecution.getWorkflowId());
      }
    }
    executionData.setAppId(app.getAppId());
    if (approvalStateType == null) {
      executionData.setApprovalStateType(USER_GROUP);
      return executeUserGroupApproval(userGroups, app.getAccountId(), placeholderValues, approvalId, executionData);
    }
    switch (approvalStateType) {
      case JIRA:
        return executeJiraApproval(context, executionData, approvalId);
      case SERVICENOW:
        return executeServiceNowApproval(context, executionData, approvalId);
      case USER_GROUP:
        return executeUserGroupApproval(userGroups, app.getAccountId(), placeholderValues, approvalId, executionData);
      case SHELL_SCRIPT:
        return executeShellScriptApproval(context, app.getAccountId(), app.getUuid(), approvalId,
            approvalStateParams.getShellScriptApprovalParams(), executionData);
      default:
        throw new WingsException(
            "Invalid ApprovalStateType, it should be one of ServiceNow, Jira, HarnessUi or Custom Shell script");
    }
  }

  private void setPipelineVariables(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Map<String, Object> variables = new HashMap<>();
    if (isNotEmpty(workflowStandardParams.getWorkflowVariables())) {
      workflowStandardParams.getWorkflowVariables().forEach((s, s2) -> { variables.put(s, s2); });
      if (isNotEmpty(variables)) {
        if (workflowStandardParams.getWorkflowElement() == null) {
          workflowStandardParams.setWorkflowElement(WorkflowElement.builder().variables(variables).build());
        } else {
          workflowStandardParams.getWorkflowElement().setVariables(variables);
        }
      }
    }
  }

  private String createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

    ActivityBuilder activityBuilder =
        Activity.builder()
            .applicationName(app.getName())
            .commandName(getName())
            .type(Type.Other)
            .workflowType(executionContext.getWorkflowType())
            .workflowExecutionName(executionContext.getWorkflowExecutionName())
            .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
            .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
            .commandType(getStateType())
            .workflowExecutionId(executionContext.getWorkflowExecutionId())
            .workflowId(executionContext.getWorkflowId())
            .commandUnits(
                asList(Builder.aCommand().withName(SCRIPT_APPROVAL_COMMAND).withCommandType(CommandType.OTHER).build()))
            .status(RUNNING)
            .triggeredBy(TriggeredBy.builder()
                             .email(workflowStandardParams.getCurrentUser().getEmail())
                             .name(workflowStandardParams.getCurrentUser().getName())
                             .build());

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType().equals(BUILD)) {
      activityBuilder.environmentId(GLOBAL_ENV_ID).environmentName(GLOBAL_ENV_ID).environmentType(ALL);
    } else {
      activityBuilder.environmentId(env != null ? env.getUuid() : ENVIRONMENT_ID)
          .environmentName(env != null ? env.getName() : ENVIRONMENT_NAME)
          .environmentType(env != null ? env.getEnvironmentType() : ALL);
    }

    Activity activity = activityBuilder.build();
    activity.setAppId(app.getUuid());
    return activityService.save(activity).getUuid();
  }

  private ExecutionResponse executeShellScriptApproval(ExecutionContext context, String accountId, String appId,
      String approvalId, ShellScriptApprovalParams parameters, ApprovalStateExecutionData executionData) {
    parameters.setScriptString(context.renderExpression(parameters.getScriptString()));
    String activityId = createActivity(context);

    ScriptApprovalJob.doRetryJob(
        serviceJobScheduler, accountId, appId, approvalId, context.getWorkflowExecutionId(), activityId, parameters);

    ExecutionResponse executionResponse = anExecutionResponse()
                                              .withAsync(true)
                                              .withExecutionStatus(RUNNING)
                                              .withErrorMessage("Waiting for Approval")
                                              .withCorrelationIds(asList(approvalId))
                                              .build();

    if (EmptyPredicate.isNotEmpty(context.getPipelineStateElementId())) {
      executionResponse.setStateExecutionData(executionData);
    } else {
      executionResponse.setStateExecutionData(
          ShellScriptApprovalExecutionData.builder().activityId(activityId).approvalId(approvalId).build());
    }

    return executionResponse;
  }

  private ExecutionResponse executeJiraApproval(
      ExecutionContext context, ApprovalStateExecutionData executionData, String approvalId) {
    JiraApprovalParams jiraApprovalParams = approvalStateParams.getJiraApprovalParams();
    jiraApprovalParams.setIssueId(context.renderExpression(jiraApprovalParams.getIssueId()));
    Application app = ((ExecutionContextImpl) context).getApp();

    executionData.setApprovalField(jiraApprovalParams.getApprovalField());
    executionData.setApprovalValue(jiraApprovalParams.getApprovalValue());
    executionData.setRejectionField(jiraApprovalParams.getRejectionField());
    executionData.setRejectionValue(jiraApprovalParams.getRejectionValue());

    // Create a cron job which polls JIRA for approval status
    JiraPollingJob.doPollingJob(serviceJobScheduler, jiraApprovalParams, executionData.getApprovalId(),
        app.getAccountId(), app.getAppId(), context.getWorkflowExecutionId());

    JiraExecutionData jiraExecutionData = jiraHelperService.createWebhook(
        jiraApprovalParams, app.getAccountId(), app.getAppId(), context.getWorkflowExecutionId(), approvalId);
    if (jiraExecutionData.getExecutionStatus().equals(FAILED)) {
      return anExecutionResponse()
          .withExecutionStatus(jiraExecutionData.getExecutionStatus())
          .withErrorMessage(jiraExecutionData.getErrorMessage())
          .withStateExecutionData(executionData)
          .build();
    }
    // issue Url on which approval is waiting in case of jira.
    executionData.setIssueUrl(jiraExecutionData.getIssueUrl());
    executionData.setWebhookUrl(jiraExecutionData.getWebhookUrl());
    return anExecutionResponse()
        .withAsync(true)
        .withExecutionStatus(PAUSED)
        .withErrorMessage(jiraExecutionData.getErrorMessage())
        .withCorrelationIds(asList(approvalId))
        .withStateExecutionData(executionData)
        .build();
  }

  private ExecutionResponse executeServiceNowApproval(
      ExecutionContext context, ApprovalStateExecutionData executionData, String approvalId) {
    ServiceNowApprovalParams servicenowApprovalParams = approvalStateParams.getServiceNowApprovalParams();
    executionData.setApprovalField(servicenowApprovalParams.getApprovalField());
    executionData.setApprovalValue(servicenowApprovalParams.getApprovalValue());
    executionData.setRejectionField(servicenowApprovalParams.getRejectionField());
    executionData.setRejectionValue(servicenowApprovalParams.getRejectionValue());

    Application app = ((ExecutionContextImpl) context).getApp();

    // Create a cron job which polls ServiceNow for approval status
    ServiceNowApprovalJob.doPollingJob(serviceJobScheduler, servicenowApprovalParams, executionData.getApprovalId(),
        app.getAccountId(), app.getAppId(), context.getWorkflowExecutionId(),
        servicenowApprovalParams.getTicketType().toString());

    try {
      String issueUrl = serviceNowService.getIssueUrl(servicenowApprovalParams.getIssueNumber(),
          servicenowApprovalParams.getSnowConnectorId(), servicenowApprovalParams.getTicketType(), app.getAppId(),
          app.getAccountId());

      executionData.setIssueUrl(issueUrl);
      return anExecutionResponse()
          .withAsync(true)
          .withExecutionStatus(PAUSED)
          .withErrorMessage("Waiting for approval on Ticket " + servicenowApprovalParams.getIssueNumber())
          .withCorrelationIds(asList(approvalId))
          .withStateExecutionData(executionData)
          .build();
    } catch (WingsException we) {
      return anExecutionResponse()
          .withExecutionStatus(FAILED)
          .withErrorMessage(we.getParams().get("message").toString())
          .withStateExecutionData(executionData)
          .build();
    }
  }

  private ExecutionResponse executeUserGroupApproval(List<String> userGroups, String accountId,
      Map<String, String> placeholderValues, String approvalId, ApprovalStateExecutionData executionData) {
    executionData.setUserGroups(userGroups);
    sendEmailToUserGroupMembers(userGroups, accountId, APPROVAL_NEEDED_NOTIFICATION, placeholderValues);
    return anExecutionResponse()
        .withAsync(true)
        .withExecutionStatus(PAUSED)
        .withCorrelationIds(asList(approvalId))
        .withStateExecutionData(executionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ApprovalStateExecutionData approvalNotifyResponse =
        (ApprovalStateExecutionData) response.values().iterator().next();

    // Close the alert
    Application app = ((ExecutionContextImpl) context).getApp();
    ApprovalNeededAlert approvalNeededAlert = ApprovalNeededAlert.builder()
                                                  .executionId(context.getWorkflowExecutionId())
                                                  .approvalId(approvalNotifyResponse.getApprovalId())
                                                  .build();
    populateApprovalAlert(approvalNeededAlert, context);
    alertService.closeAlert(app.getAccountId(), app.getUuid(), ApprovalNeeded, approvalNeededAlert);

    if (context.getStateExecutionData() instanceof ShellScriptApprovalExecutionData) {
      return handleAsyncShellScript(context, context.getStateExecutionData(), approvalNotifyResponse);
    }

    ApprovalStateExecutionData executionData = (ApprovalStateExecutionData) context.getStateExecutionData();
    executionData.setApprovedBy(approvalNotifyResponse.getApprovedBy());
    executionData.setComments(approvalNotifyResponse.getComments());
    executionData.setApprovedOn(System.currentTimeMillis());

    Map<String, String> placeholderValues = new HashMap<>();
    if (approvalNotifyResponse.getApprovedBy() != null) {
      placeholderValues = getPlaceholderValues(
          context, approvalNotifyResponse.getApprovedBy().getName(), approvalNotifyResponse.getStatus());
    }
    sendApprovalNotification(app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues);
    if (approvalStateType == null) {
      return handleAsyncUserGroup(userGroups, placeholderValues, context, executionData, approvalNotifyResponse);
    }
    switch (approvalStateType) {
      case JIRA:
        return handleAsyncJira(context, executionData, approvalNotifyResponse);
      case SERVICENOW:
        return handleAsyncServiceNow(context, executionData, approvalNotifyResponse);
      case USER_GROUP:
        return handleAsyncUserGroup(userGroups, placeholderValues, context, executionData, approvalNotifyResponse);
      case SHELL_SCRIPT:
        return handleAsyncShellScript(context, context.getStateExecutionData(), approvalNotifyResponse);
      default:
        throw new WingsException("Invalid ApprovalStateType");
    }
  }

  private ExecutionResponse handleAsyncShellScript(
      ExecutionContext context, StateExecutionData executionData, ApprovalStateExecutionData approvalNotifyResponse) {
    String errorMessage = "";
    if (approvalNotifyResponse.getStatus() == REJECTED) {
      errorMessage = "Rejected by Script";
    } else if (approvalNotifyResponse.getStatus() == SUCCESS) {
      errorMessage = "Approved by Script";
    } else {
      errorMessage = "Waiting for Approval";
    }

    setPipelineVariables(context);

    // If the Approval is in a pipeline, ApprovalStateExecutionData is expected.
    if (executionData instanceof ShellScriptApprovalExecutionData) {
      return anExecutionResponse()
          .withStateExecutionData(executionData)
          .withExecutionStatus(approvalNotifyResponse.getStatus())
          .withErrorMessage(errorMessage)
          .build();
    } else {
      ApprovalStateExecutionData approvalStateExecutionData = ApprovalStateExecutionData.builder()
                                                                  .approvalId(approvalNotifyResponse.getApprovalId())
                                                                  .appId(context.getAppId())
                                                                  .comments(errorMessage)
                                                                  .build();

      if (approvalNotifyResponse.getStatus() == SUCCESS || approvalNotifyResponse.getStatus() == REJECTED) {
        approvalStateExecutionData.setApprovedOn(approvalNotifyResponse.getApprovedOn());
      }

      return anExecutionResponse()
          .withStateExecutionData(approvalStateExecutionData)
          .withExecutionStatus(approvalNotifyResponse.getStatus())
          .withErrorMessage(errorMessage)
          .build();
    }
  }

  private ExecutionResponse handleAsyncJira(ExecutionContext context, ApprovalStateExecutionData executionData,
      ApprovalStateExecutionData approvalNotifyResponse) {
    JiraApprovalParams jiraApprovalParams = approvalStateParams.getJiraApprovalParams();
    setPipelineVariables(context);
    jiraApprovalParams.setIssueId(context.renderExpression(jiraApprovalParams.getIssueId()));
    Application app = ((ExecutionContextImpl) context).getApp();
    JiraExecutionData jiraExecutionData = jiraHelperService.deleteWebhook(
        jiraApprovalParams, executionData.getWebhookUrl(), app.getAppId(), app.getAccountId());
    // issue Url on which approval was waiting.
    executionData.setIssueUrl(jiraExecutionData.getIssueUrl());
    return anExecutionResponse()
        .withStateExecutionData(executionData)
        .withExecutionStatus(approvalNotifyResponse.getStatus())
        .withErrorMessage(jiraExecutionData.getErrorMessage())
        .build();
  }

  private ExecutionResponse handleAsyncServiceNow(ExecutionContext context, ApprovalStateExecutionData executionData,
      ApprovalStateExecutionData approvalNotifyResponse) {
    ServiceNowApprovalParams servicenowApprovalParams = approvalStateParams.getServiceNowApprovalParams();
    setPipelineVariables(context);
    return anExecutionResponse()
        .withStateExecutionData(executionData)
        .withExecutionStatus(approvalNotifyResponse.getStatus())
        .withErrorMessage("Approval/Rejection provided on ticket: " + servicenowApprovalParams.getIssueNumber())
        .build();
  }

  private ExecutionResponse handleAsyncUserGroup(List<String> userGroups, Map<String, String> placeholderValues,
      ExecutionContext context, ApprovalStateExecutionData executionData,
      ApprovalStateExecutionData approvalNotifyResponse) {
    Application app = ((ExecutionContextImpl) context).getApp();
    sendEmailToUserGroupMembers(userGroups, app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues);
    return anExecutionResponse()
        .withStateExecutionData(executionData)
        .withExecutionStatus(approvalNotifyResponse.getStatus())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    if (context == null || context.getStateExecutionData() == null) {
      return;
    }

    Application app = ((ExecutionContextImpl) context).getApp();
    Integer timeout = getTimeoutMillis();
    Long startTimeMillis = context.getStateExecutionData().getStartTs();
    Long currentTimeMillis = System.currentTimeMillis();
    NotificationMessageType notificationMessageType;

    String errorMsg = "";
    String approvalType = "";
    if (((ExecutionContextImpl) context).getStateExecutionInstance() != null
        && ((ExecutionContextImpl) context).getStateExecutionInstance().getExecutionType() != null) {
      approvalType = notificationMessageResolver.getApprovalType(
          ((ExecutionContextImpl) context).getStateExecutionInstance().getExecutionType());
    }

    Map<String, String> placeholderValues;
    if (currentTimeMillis >= (timeout + startTimeMillis)) {
      if (approvalType != null && approvalType.equalsIgnoreCase("PIPELINE")) {
        errorMsg = "Pipeline was not approved within " + Misc.getDurationString(getTimeoutMillis());
      } else if (approvalType != null && approvalType.equalsIgnoreCase("ORCHESTRATION")) {
        errorMsg = "Workflow was not approved within " + Misc.getDurationString(getTimeoutMillis());
      } else {
        errorMsg = "Approval not approved within " + Misc.getDurationString(getTimeoutMillis());
      }
      notificationMessageType = APPROVAL_EXPIRED_NOTIFICATION;
      placeholderValues = getPlaceholderValues(context, errorMsg);
      sendApprovalNotification(app.getAccountId(), notificationMessageType, placeholderValues);
    } else {
      if (approvalType != null && approvalType.equalsIgnoreCase("PIPELINE")) {
        errorMsg = "Pipeline was aborted";
      } else if (approvalType != null && approvalType.equalsIgnoreCase("ORCHESTRATION")) {
        errorMsg = "Workflow was aborted";
      } else {
        errorMsg = "Workflow or Pipeline was aborted";
      }

      notificationMessageType = APPROVAL_STATE_CHANGE_NOTIFICATION;
      User user = UserThreadLocal.get();
      String userName = (user != null && user.getName() != null) ? user.getName() : "System";
      placeholderValues = getPlaceholderValues(context, userName, ABORTED);
      sendApprovalNotification(app.getAccountId(), notificationMessageType, placeholderValues);
    }

    context.getStateExecutionData().setErrorMsg(errorMsg);
    if (approvalStateType == null) {
      sendEmailToUserGroupMembers(userGroups, app.getAccountId(), notificationMessageType, placeholderValues);
      return;
    }
    switch (approvalStateType) {
      case JIRA:
        handleAbortEventJira(context, app);
        return;
      case SERVICENOW:
        handleAbortEventServiceNow(context, app);
        return;
      case USER_GROUP:
        sendEmailToUserGroupMembers(userGroups, app.getAccountId(), notificationMessageType, placeholderValues);
        return;
      case SHELL_SCRIPT:
        handleAbortScriptApproval(context.getStateExecutionData());
        return;
      default:
        throw new WingsException("Invalid ApprovalStateType : neither JIRA nor USER_GROUP");
    }
  }

  private void handleAbortScriptApproval(StateExecutionData stateExecutionData) {
    ShellScriptApprovalExecutionData executionData = (ShellScriptApprovalExecutionData) stateExecutionData;
    serviceJobScheduler.deleteJob(executionData.getApprovalId(), SCRIPT_APPROVAL_JOB_GROUP);
  }

  private void handleAbortEventJira(ExecutionContext context, Application app) {
    ApprovalStateExecutionData executionData = (ApprovalStateExecutionData) context.getStateExecutionData();
    jiraHelperService.deleteWebhook(
        approvalStateParams.getJiraApprovalParams(), executionData.getWebhookUrl(), app.getAppId(), app.getAccountId());
    // Todo@Pooja : delete JiraPolling job in case of pipeline abort.
  }

  private void handleAbortEventServiceNow(ExecutionContext context, Application app) {
    ApprovalStateExecutionData executionData = (ApprovalStateExecutionData) context.getStateExecutionData();
    ServiceNowApprovalJob.deleteJob(serviceJobScheduler, executionData.getApprovalId());
  }

  @Override
  public Integer getTimeoutMillis() {
    if (super.getTimeoutMillis() == null) {
      return DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS;
    }
    return super.getTimeoutMillis();
  }

  void sendApprovalNotification(
      String accountId, NotificationMessageType notificationMessageType, Map<String, String> placeHolderValues) {
    List<NotificationGroup> notificationGroups = notificationSetupService.listDefaultNotificationGroup(accountId);
    NotificationRule notificationRule = aNotificationRule().withNotificationGroups(notificationGroups).build();

    notificationService.sendNotificationAsync(anInformationNotification()
                                                  .withAppId(GLOBAL_APP_ID)
                                                  .withAccountId(accountId)
                                                  .withNotificationTemplateId(notificationMessageType.name())
                                                  .withNotificationTemplateVariables(placeHolderValues)
                                                  .build(),
        singletonList(notificationRule));
  }

  private static String getStatusMessage(ExecutionStatus status) {
    switch (status) {
      case SUCCESS:
        return "approved";
      case ABORTED:
        return "aborted";
      case REJECTED:
        return "rejected";
      case EXPIRED:
        return "expired";
      case PAUSED:
        return "paused";
      default:
        unhandled(status);
        return "failed";
    }
  }

  Map<String, String> getPlaceholderValues(ExecutionContext context, String userName, ExecutionStatus status) {
    WorkflowExecution workflowExecution = workflowExecutionService.getWorkflowExecution(
        ((ExecutionContextImpl) context).getApp().getUuid(), context.getWorkflowExecutionId());

    String statusMsg = getStatusMessage(status);
    long startTs = (status == PAUSED) ? workflowExecution.getCreatedAt() : context.getStateExecutionData().getStartTs();
    if (status == PAUSED) {
      userName = workflowExecution.getTriggeredBy().getName();
    }

    return notificationMessageResolver.getPlaceholderValues(
        context, userName, startTs, System.currentTimeMillis(), "", statusMsg, "", status, ApprovalNeeded);
  }

  Map<String, String> getPlaceholderValues(ExecutionContext context, String timeout) {
    return notificationMessageResolver.getPlaceholderValues(
        context, "", 0, 0, timeout, "", "", EXPIRED, ApprovalNeeded);
  }

  void sendEmailToUserGroupMembers(List<String> userGroups, String accountId,
      NotificationMessageType notificationMessageType, Map<String, String> placeHolderValues) {
    List<String> userEmailAddress = getUserGroupMemberEmailAddresses(accountId, userGroups);
    if (isEmpty(userEmailAddress)) {
      return;
    }

    List<String> excludeEmailAddress = getNotificationGroupMemberEmailAddresses(accountId);
    userEmailAddress.removeAll(excludeEmailAddress);
    if (isEmpty(userEmailAddress)) {
      return;
    }

    EmailData emailData =
        notificationDispatcherService.obtainEmailData(notificationMessageType.toString(), placeHolderValues);
    if (isEmpty(emailData.getBody()) || isEmpty(emailData.getSubject())) {
      return;
    }

    emailData.setSystem(true);
    emailData.setCc(Collections.emptyList());
    emailData.setTo(userEmailAddress);
    emailNotificationService.sendAsync(emailData);
  }

  private List<String> getNotificationGroupMemberEmailAddresses(String accountId) {
    List<NotificationGroup> notificationGroups = notificationSetupService.listDefaultNotificationGroup(accountId);
    return notificationSetupService.getUserEmailAddressFromNotificationGroups(accountId, notificationGroups);
  }

  private List<String> getUserGroupMemberEmailAddresses(String accountId, List<String> userGroups) {
    if (isEmpty(userGroups)) {
      return asList();
    }

    List<String> userGroupMembers = userGroupService.fetchUserGroupsMemberIds(accountId, userGroups);
    if (isEmpty(userGroupMembers)) {
      return asList();
    }

    return userService.fetchUserEmailAddressesFromUserIds(userGroupMembers);
  }

  void populateApprovalAlert(ApprovalNeededAlert approvalNeededAlert, ExecutionContext context) {
    if (((ExecutionContextImpl) context).getStateExecutionInstance() != null
        && ((ExecutionContextImpl) context).getStateExecutionInstance().getExecutionType() != null) {
      approvalNeededAlert.setWorkflowType(
          ((ExecutionContextImpl) context).getStateExecutionInstance().getExecutionType());
    }

    if (((ExecutionContextImpl) context).getEnv() != null) {
      approvalNeededAlert.setEnvId(((ExecutionContextImpl) context).getEnv().getUuid());
    }

    if (approvalNeededAlert.getWorkflowType() == null) {
      throw new InvalidRequestException("Workflow type cannot be null");
    }

    WorkflowType workflowType = approvalNeededAlert.getWorkflowType();
    switch (workflowType) {
      case PIPELINE: {
        approvalNeededAlert.setPipelineExecutionId(context.getWorkflowExecutionId());
        break;
      }
      case ORCHESTRATION: {
        approvalNeededAlert.setWorkflowExecutionId(context.getWorkflowExecutionId());
        WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

        if (workflowStandardParams != null && workflowStandardParams.getWorkflowElement() != null) {
          approvalNeededAlert.setPipelineExecutionId(
              workflowStandardParams.getWorkflowElement().getPipelineDeploymentUuid());
        }
        break;
      }
      default:
        unhandled(workflowType);
    }
  }
}
