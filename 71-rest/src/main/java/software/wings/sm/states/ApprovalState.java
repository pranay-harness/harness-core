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
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.event.model.EventConstants.ENVIRONMENT_ID;
import static io.harness.event.model.EventConstants.ENVIRONMENT_NAME;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.alert.AlertType.ApprovalNeeded;
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
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.scheduler.PersistentScheduler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.ServiceNowExecutionData;
import software.wings.api.WorkflowElement;
import software.wings.api.jira.JiraExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InformationNotification;
import software.wings.beans.NotificationRule;
import software.wings.beans.NotificationRule.NotificationRuleBuilder;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.beans.approval.ApprovalPollingJobEntity;
import software.wings.beans.approval.ApprovalStateParams;
import software.wings.beans.approval.JiraApprovalParams;
import software.wings.beans.approval.ServiceNowApprovalParams;
import software.wings.beans.approval.ShellScriptApprovalParams;
import software.wings.beans.command.Command.Builder;
import software.wings.beans.command.CommandType;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.scheduler.JiraPollingJob;
import software.wings.scheduler.ServiceNowApprovalJob;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.impl.workflow.WorkflowNotificationHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.ApprovalPolingService;
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
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ApprovalState extends State {
  @Getter @Setter private String groupName;
  @Getter @Setter private List<String> userGroups = new ArrayList<>();
  @Getter @Setter private boolean disable;

  public enum ApprovalStateType { JIRA, USER_GROUP, SHELL_SCRIPT, SERVICENOW }

  @Inject private AlertService alertService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject private NotificationDispatcherService notificationDispatcherService;
  @Inject private NotificationService notificationService;
  @Inject private UserGroupService userGroupService;
  @Inject private UserService userService;
  @Inject private JiraHelperService jiraHelperService;
  @Inject private ServiceNowService serviceNowService;
  @Inject private transient ActivityService activityService;
  @Inject private transient WorkflowNotificationHelper workflowNotificationHelper;
  @Inject private ApprovalPolingService approvalPolingService;

  @Inject @Named("ServiceJobScheduler") private PersistentScheduler serviceJobScheduler;
  private Integer DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS = 7 * 24 * 60 * 60 * 1000; // 7 days
  private String SCRIPT_APPROVAL_COMMAND = "Execute Approval Script";
  private String SCRIPT_APPROVAL_JOB_GROUP = "SHELL_SCRIPT_APPROVAL_JOB";
  public static final String APPROVAL_STATE_TYPE_VARIABLE = "approvalStateType";
  public static final String USER_GROUPS_VARIABLE = "userGroups";

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

    Application app = context.getApp();
    Map<String, String> placeholderValues = getPlaceholderValues(context, "", PAUSED);
    // Open an alert
    ApprovalNeededAlert approvalNeededAlert = ApprovalNeededAlert.builder()
                                                  .executionId(context.getWorkflowExecutionId())
                                                  .approvalId(approvalId)
                                                  .name(context.getWorkflowExecutionName())
                                                  .build();
    populateApprovalAlert(approvalNeededAlert, context);
    alertService.openAlert(app.getAccountId(), app.getUuid(), ApprovalNeeded, approvalNeededAlert);

    try {
      workflowNotificationHelper.sendApprovalNotification(
          app.getAccountId(), APPROVAL_NEEDED_NOTIFICATION, placeholderValues, context);
    } catch (Exception e) {
      // catch exception so that failure to send notification doesn't affect rest of execution
      logger.error("Error sending approval notifiaction. accountId={}", app.getAccountId(), e);
    }

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
      return executeUserGroupApproval(
          userGroups, app.getAccountId(), placeholderValues, approvalId, executionData, app.getUuid());
    }
    switch (approvalStateType) {
      case JIRA:
        return executeJiraApproval(context, executionData, approvalId);
      case SERVICENOW:
        return executeServiceNowApproval(context, executionData, approvalId);
      case USER_GROUP:
        return executeUserGroupApproval(
            userGroups, app.getAccountId(), placeholderValues, approvalId, executionData, app.getUuid());
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
    Application app = executionContext.getApp();
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
    executionData.setActivityId(activityId);

    ApprovalPollingJobEntity approvalPollingJobEntity =
        ApprovalPollingJobEntity.builder()
            .accountId(accountId)
            .appId(appId)
            .approvalId(approvalId)
            .stateExecutionInstanceId(context.getStateExecutionInstanceId())
            .workflowExecutionId(context.getWorkflowExecutionId())
            .activityId(activityId)
            .scriptString(parameters.getScriptString())
            .approvalType(approvalStateType)
            .build();

    try {
      approvalPolingService.save(approvalPollingJobEntity);
      return anExecutionResponse()
          .withAsync(true)
          .withExecutionStatus(PAUSED)
          .withErrorMessage("Waiting for Approval")
          .withCorrelationIds(singletonList(approvalId))
          .withStateExecutionData(executionData)
          .build();
    } catch (WingsException e) {
      return anExecutionResponse()
          .withExecutionStatus(FAILED)
          .withErrorMessage("Failed to schedule Approval" + e.getMessage())
          .withStateExecutionData(executionData)
          .build();
    }
  }

  private ExecutionResponse executeJiraApproval(
      ExecutionContext context, ApprovalStateExecutionData executionData, String approvalId) {
    JiraApprovalParams jiraApprovalParams = approvalStateParams.getJiraApprovalParams();
    jiraApprovalParams.setIssueId(context.renderExpression(jiraApprovalParams.getIssueId()));

    if (ExpressionEvaluator.containsVariablePattern(jiraApprovalParams.getIssueId())) {
      return anExecutionResponse()
          .withExecutionStatus(FAILED)
          .withErrorMessage("Expression not rendered for Jira issue Id: " + jiraApprovalParams.getIssueId())
          .withStateExecutionData(executionData)
          .build();
    }

    Application app = context.getApp();

    executionData.setApprovalField(jiraApprovalParams.getApprovalField());
    executionData.setApprovalValue(jiraApprovalParams.getApprovalValue());
    executionData.setRejectionField(jiraApprovalParams.getRejectionField());
    executionData.setRejectionValue(jiraApprovalParams.getRejectionValue());

    JiraExecutionData jiraExecutionData = jiraHelperService.fetchIssue(
        jiraApprovalParams, app.getAccountId(), app.getAppId(), context.getWorkflowExecutionId(), approvalId);

    if (jiraExecutionData.getExecutionStatus().equals(FAILED)) {
      return anExecutionResponse()
          .withExecutionStatus(FAILED)
          .withErrorMessage(jiraExecutionData.getErrorMessage())
          .withStateExecutionData(executionData)
          .build();
    }

    executionData.setIssueUrl(jiraExecutionData.getIssueUrl());
    executionData.setIssueKey(jiraExecutionData.getIssueKey());
    executionData.setCurrentStatus(jiraExecutionData.getCurrentStatus());

    if (jiraExecutionData.getCurrentStatus().equalsIgnoreCase(jiraApprovalParams.getApprovalValue())) {
      return anExecutionResponse()
          .withExecutionStatus(SUCCESS)
          .withErrorMessage("Approval provided on ticket: " + jiraExecutionData.getIssueKey())
          .withStateExecutionData(executionData)
          .build();
    }

    if (jiraApprovalParams.getRejectionValue() != null
        && jiraExecutionData.getCurrentStatus().equalsIgnoreCase(jiraApprovalParams.getRejectionValue())) {
      return anExecutionResponse()
          .withExecutionStatus(REJECTED)
          .withErrorMessage("Rejection provided on ticket: " + jiraExecutionData.getIssueKey())
          .withStateExecutionData(executionData)
          .build();
    }

    // Create a cron job which polls JIRA for approval status
    logger.info("IssueId = {} while creating Jira polling Job", jiraApprovalParams.getIssueId());
    ApprovalPollingJobEntity approvalPollingJobEntity =
        ApprovalPollingJobEntity.builder()
            .accountId(app.getAccountId())
            .appId(app.getAppId())
            .approvalField(jiraApprovalParams.getApprovalField())
            .rejectionField(jiraApprovalParams.getRejectionField())
            .approvalValue(jiraApprovalParams.getApprovalValue())
            .rejectionValue(jiraApprovalParams.getRejectionValue())
            .approvalId(approvalId)
            .approvalType(approvalStateType)
            .connectorId(jiraApprovalParams.getJiraConnectorId())
            .issueId(jiraApprovalParams.getIssueId())
            .stateExecutionInstanceId(context.getStateExecutionInstanceId())
            .workflowExecutionId(context.getWorkflowExecutionId())
            .build();
    try {
      approvalPolingService.save(approvalPollingJobEntity);
      return anExecutionResponse()
          .withAsync(true)
          .withExecutionStatus(PAUSED)
          .withErrorMessage(jiraExecutionData.getErrorMessage())
          .withCorrelationIds(asList(approvalId))
          .withStateExecutionData(executionData)
          .build();
    } catch (WingsException e) {
      return anExecutionResponse()
          .withExecutionStatus(FAILED)
          .withErrorMessage("Failed to schedule Approval" + e.getMessage())
          .withStateExecutionData(executionData)
          .build();
    }
  }

  private ExecutionResponse executeServiceNowApproval(
      ExecutionContext context, ApprovalStateExecutionData executionData, String approvalId) {
    ServiceNowApprovalParams servicenowApprovalParams = approvalStateParams.getServiceNowApprovalParams();
    servicenowApprovalParams.setIssueNumber(context.renderExpression(servicenowApprovalParams.getIssueNumber()));

    if (ExpressionEvaluator.containsVariablePattern(servicenowApprovalParams.getIssueNumber())) {
      return anExecutionResponse()
          .withExecutionStatus(FAILED)
          .withErrorMessage("Expression not rendered for issue Number: " + servicenowApprovalParams.getIssueNumber())
          .withStateExecutionData(executionData)
          .build();
    }

    executionData.setApprovalField(servicenowApprovalParams.getApprovalField());
    executionData.setApprovalValue(servicenowApprovalParams.getApprovalValue());
    executionData.setRejectionField(servicenowApprovalParams.getRejectionField());
    executionData.setRejectionValue(servicenowApprovalParams.getRejectionValue());

    Application app = context.getApp();

    try {
      ServiceNowExecutionData serviceNowExecutionData = serviceNowService.getIssueUrl(
          servicenowApprovalParams.getIssueNumber(), servicenowApprovalParams.getSnowConnectorId(),
          servicenowApprovalParams.getTicketType(), app.getAppId(), app.getAccountId());
      executionData.setTicketUrl(serviceNowExecutionData.getIssueUrl());
      executionData.setCurrentStatus(serviceNowExecutionData.getCurrentState());
      executionData.setTicketType(servicenowApprovalParams.getTicketType());

      if (serviceNowExecutionData.getCurrentState().equalsIgnoreCase(servicenowApprovalParams.getApprovalValue())) {
        return anExecutionResponse()
            .withExecutionStatus(SUCCESS)
            .withErrorMessage("Approval provided on ticket: " + servicenowApprovalParams.getIssueNumber())
            .withStateExecutionData(executionData)
            .build();
      }

      if (servicenowApprovalParams.getRejectionValue() != null
          && serviceNowExecutionData.getCurrentState().equalsIgnoreCase(servicenowApprovalParams.getRejectionValue())) {
        return anExecutionResponse()
            .withExecutionStatus(REJECTED)
            .withErrorMessage("Rejection provided on ticket: " + servicenowApprovalParams.getIssueNumber())
            .withStateExecutionData(executionData)
            .build();
      }

    } catch (WingsException we) {
      return anExecutionResponse()
          .withExecutionStatus(FAILED)
          .withErrorMessage(we.getParams().get("message").toString())
          .withStateExecutionData(executionData)
          .build();
    }

    // Create a cron job which polls ServiceNow for approval status
    logger.info("IssueId = {} while creating ServiceNow polling Job", servicenowApprovalParams.getIssueNumber());
    ApprovalPollingJobEntity approvalPollingJobEntity =
        ApprovalPollingJobEntity.builder()
            .accountId(app.getAccountId())
            .appId(app.getAppId())
            .approvalField(servicenowApprovalParams.getApprovalField())
            .rejectionField(servicenowApprovalParams.getRejectionField())
            .approvalValue(servicenowApprovalParams.getApprovalValue())
            .rejectionValue(servicenowApprovalParams.getRejectionValue())
            .approvalId(approvalId)
            .approvalType(approvalStateType)
            .connectorId(servicenowApprovalParams.getSnowConnectorId())
            .issueNumber(servicenowApprovalParams.getIssueNumber())
            .issueType(servicenowApprovalParams.getTicketType())
            .stateExecutionInstanceId(context.getStateExecutionInstanceId())
            .workflowExecutionId(context.getWorkflowExecutionId())
            .build();

    try {
      approvalPolingService.save(approvalPollingJobEntity);
      return anExecutionResponse()
          .withAsync(true)
          .withExecutionStatus(PAUSED)
          .withErrorMessage("Waiting for approval on Ticket " + servicenowApprovalParams.getIssueNumber())
          .withCorrelationIds(asList(approvalId))
          .withStateExecutionData(executionData)
          .build();
    } catch (WingsException e) {
      return anExecutionResponse()
          .withExecutionStatus(FAILED)
          .withErrorMessage("Failed to schedule Approval" + e.getMessage())
          .withStateExecutionData(executionData)
          .build();
    }
  }

  private ExecutionResponse executeUserGroupApproval(List<String> userGroups, String accountId,
      Map<String, String> placeholderValues, String approvalId, ApprovalStateExecutionData executionData,
      String appId) {
    executionData.setUserGroups(userGroups);
    sendNotificationForUserGroupApproval(userGroups, appId, accountId, APPROVAL_NEEDED_NOTIFICATION, placeholderValues);
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
    Application app = context.getApp();
    ApprovalNeededAlert approvalNeededAlert = ApprovalNeededAlert.builder()
                                                  .executionId(context.getWorkflowExecutionId())
                                                  .approvalId(approvalNotifyResponse.getApprovalId())
                                                  .build();
    populateApprovalAlert(approvalNeededAlert, context);
    alertService.closeAlert(app.getAccountId(), app.getUuid(), ApprovalNeeded, approvalNeededAlert);

    ApprovalStateExecutionData executionData = (ApprovalStateExecutionData) context.getStateExecutionData();
    executionData.setApprovedBy(approvalNotifyResponse.getApprovedBy());
    executionData.setComments(approvalNotifyResponse.getComments());
    executionData.setApprovedOn(System.currentTimeMillis());
    executionData.setCurrentStatus(approvalNotifyResponse.getCurrentStatus());

    Map<String, String> placeholderValues;
    if (approvalNotifyResponse.getApprovedBy() != null) {
      placeholderValues = getPlaceholderValues(
          context, approvalNotifyResponse.getApprovedBy().getName(), approvalNotifyResponse.getStatus());
    } else {
      placeholderValues = getPlaceholderValues(context, "", approvalNotifyResponse.getStatus());
    }

    workflowNotificationHelper.sendApprovalNotification(
        app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues, context);
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
        return handleAsyncShellScript(context, executionData, approvalNotifyResponse);
      default:
        throw new WingsException("Invalid ApprovalStateType");
    }
  }

  private ExecutionResponse handleAsyncShellScript(ExecutionContext context, ApprovalStateExecutionData executionData,
      ApprovalStateExecutionData approvalNotifyResponse) {
    String errorMessage;
    if (approvalNotifyResponse.getStatus() == REJECTED) {
      errorMessage = "Rejected by Script";
    } else if (approvalNotifyResponse.getStatus() == SUCCESS) {
      errorMessage = "Approved by Script";
    } else {
      errorMessage = "Waiting for Approval";
    }

    setPipelineVariables(context);
    approvalPolingService.delete(executionData.getApprovalId());

    return anExecutionResponse()
        .withStateExecutionData(executionData)
        .withExecutionStatus(approvalNotifyResponse.getStatus())
        .withErrorMessage(errorMessage)
        .build();
  }

  private ExecutionResponse handleAsyncJira(ExecutionContext context, ApprovalStateExecutionData executionData,
      ApprovalStateExecutionData approvalNotifyResponse) {
    JiraApprovalParams jiraApprovalParams = approvalStateParams.getJiraApprovalParams();
    setPipelineVariables(context);
    jiraApprovalParams.setIssueId(context.renderExpression(jiraApprovalParams.getIssueId()));
    logger.info("Deleting job for approvalId: {}, workflowExecutionId: {} ", executionData.getApprovalId(),
        executionData.getWorkflowId());
    approvalPolingService.delete(executionData.getApprovalId());
    // Todo: keeping this for backward compatibility and cleanup of old jobs. Can be removed a disconnected onprem
    // release
    JiraPollingJob.deleteJob(serviceJobScheduler, executionData.getApprovalId());

    return anExecutionResponse()
        .withStateExecutionData(executionData)
        .withExecutionStatus(approvalNotifyResponse.getStatus())
        .withErrorMessage(approvalNotifyResponse.getErrorMsg() + executionData.getIssueKey())
        .build();
  }

  private ExecutionResponse handleAsyncServiceNow(ExecutionContext context, ApprovalStateExecutionData executionData,
      ApprovalStateExecutionData approvalNotifyResponse) {
    ServiceNowApprovalParams servicenowApprovalParams = approvalStateParams.getServiceNowApprovalParams();
    servicenowApprovalParams.setIssueNumber(context.renderExpression(servicenowApprovalParams.getIssueNumber()));

    logger.info("Deleting job for approvalId: {}, workflowExecutionId: {} ", executionData.getApprovalId(),
        executionData.getWorkflowId());
    approvalPolingService.delete(executionData.getApprovalId());
    // Todo: keeping this for backward compatibility and cleanup of old jobs. Can be removed a disconnected onprem
    // release
    ServiceNowApprovalJob.deleteJob(serviceJobScheduler, executionData.getApprovalId());

    setPipelineVariables(context);
    return anExecutionResponse()
        .withStateExecutionData(executionData)
        .withExecutionStatus(approvalNotifyResponse.getStatus())
        .withErrorMessage(approvalNotifyResponse.getErrorMsg() + servicenowApprovalParams.getIssueNumber())
        .build();
  }

  private ExecutionResponse handleAsyncUserGroup(List<String> userGroups, Map<String, String> placeholderValues,
      ExecutionContext context, ApprovalStateExecutionData executionData,
      ApprovalStateExecutionData approvalNotifyResponse) {
    Application app = context.getApp();
    sendNotificationForUserGroupApproval(
        userGroups, app.getUuid(), app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues);
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

    Application app = context.getApp();
    Integer timeout = getTimeoutMillis();
    Long startTimeMillis = context.getStateExecutionData().getStartTs();
    long currentTimeMillis = System.currentTimeMillis();
    NotificationMessageType notificationMessageType;

    String errorMsg;
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
      workflowNotificationHelper.sendApprovalNotification(
          app.getAccountId(), notificationMessageType, placeholderValues, context);
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
      workflowNotificationHelper.sendApprovalNotification(
          app.getAccountId(), notificationMessageType, placeholderValues, context);
    }

    context.getStateExecutionData().setErrorMsg(errorMsg);
    if (approvalStateType == null) {
      sendNotificationForUserGroupApproval(
          userGroups, app.getUuid(), app.getAccountId(), notificationMessageType, placeholderValues);
      return;
    }
    switch (approvalStateType) {
      case JIRA:
        handleAbortEventJira(context);
        return;
      case SERVICENOW:
        handleAbortEventServiceNow(context);
        return;
      case USER_GROUP:
        sendNotificationForUserGroupApproval(
            userGroups, app.getUuid(), app.getAccountId(), notificationMessageType, placeholderValues);
        return;
      case SHELL_SCRIPT:
        handleAbortScriptApproval(context);
        return;
      default:
        throw new WingsException("Invalid ApprovalStateType : neither JIRA nor USER_GROUP");
    }
  }

  private void sendNotificationForUserGroupApproval(List<String> approvalUserGroups, String appId, String accountId,
      NotificationMessageType notificationMessageType, Map<String, String> placeHolderValues) {
    NotificationRule rule = NotificationRuleBuilder.aNotificationRule().withUserGroupIds(approvalUserGroups).build();

    InformationNotification notification = anInformationNotification()
                                               .withAppId(appId)
                                               .withAccountId(accountId)
                                               .withNotificationTemplateId(notificationMessageType.name())
                                               .withNotificationTemplateVariables(placeHolderValues)
                                               .build();

    notificationService.sendNotificationAsync(notification, Collections.singletonList(rule));
  }

  private void handleAbortScriptApproval(ExecutionContext context) {
    ApprovalStateExecutionData executionData = (ApprovalStateExecutionData) context.getStateExecutionData();
    approvalPolingService.delete(executionData.getApprovalId());
    // left to clean up the old jobs. Remove later.
    serviceJobScheduler.deleteJob(executionData.getApprovalId(), SCRIPT_APPROVAL_JOB_GROUP);
  }

  private void handleAbortEventJira(ExecutionContext context) {
    ApprovalStateExecutionData executionData = (ApprovalStateExecutionData) context.getStateExecutionData();
    approvalPolingService.delete(executionData.getApprovalId());
    // Todo: keeping this for backward compatibility and cleanup of old jobs. Can be removed a disconnected onprem
    // release
    JiraPollingJob.deleteJob(serviceJobScheduler, executionData.getApprovalId());
  }

  private void handleAbortEventServiceNow(ExecutionContext context) {
    ApprovalStateExecutionData executionData = (ApprovalStateExecutionData) context.getStateExecutionData();
    approvalPolingService.delete(executionData.getApprovalId());

    // Todo: keeping this for backward compatibility and cleanup of old jobs. Can be removed a disconnected onprem
    // release
    ServiceNowApprovalJob.deleteJob(serviceJobScheduler, executionData.getApprovalId());
  }

  @Override
  public Integer getTimeoutMillis() {
    if (super.getTimeoutMillis() == null) {
      return DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS;
    }
    return super.getTimeoutMillis();
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
    WorkflowExecution workflowExecution =
        workflowExecutionService.getWorkflowExecution(context.getApp().getUuid(), context.getWorkflowExecutionId());

    String statusMsg = getStatusMessage(status);
    long startTs = (status == PAUSED) ? workflowExecution.getCreatedAt() : context.getStateExecutionData().getStartTs();
    if (status == PAUSED) {
      userName = workflowExecution.getTriggeredBy().getName();
    }

    return notificationMessageResolver.getPlaceholderValues(
        context, userName, startTs, System.currentTimeMillis(), "", statusMsg, "", status, ApprovalNeeded);
  }

  private Map<String, String> getPlaceholderValues(ExecutionContext context, String timeout) {
    return notificationMessageResolver.getPlaceholderValues(
        context, "", 0, 0, timeout, "", "", EXPIRED, ApprovalNeeded);
  }

  private void populateApprovalAlert(ApprovalNeededAlert approvalNeededAlert, ExecutionContext context) {
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
