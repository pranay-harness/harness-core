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
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.alert.AlertType.ApprovalNeeded;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_EXPIRED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_NEEDED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_STATE_CHANGE_NOTIFICATION;
import static software.wings.security.SecretManager.JWT_CATEGORY.EXTERNAL_SERVICE_SECRET;
import static software.wings.service.impl.slack.SlackApprovalUtils.createSlackApprovalMessage;
import static software.wings.sm.states.ApprovalState.ApprovalStateType.USER_GROUP;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.beans.TriggeredBy;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.scheduler.PersistentScheduler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.ApprovalStateExecutionData.ApprovalStateExecutionDataKeys;
import software.wings.api.ServiceNowExecutionData;
import software.wings.api.WorkflowElement;
import software.wings.api.jira.JiraExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EnvSummary;
import software.wings.beans.Environment;
import software.wings.beans.InformationNotification;
import software.wings.beans.NameValuePair;
import software.wings.beans.NameValuePair.NameValuePairKeys;
import software.wings.beans.NotificationRule;
import software.wings.beans.NotificationRule.NotificationRuleBuilder;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.beans.approval.ApprovalPollingJobEntity;
import software.wings.beans.approval.ApprovalStateParams;
import software.wings.beans.approval.JiraApprovalParams;
import software.wings.beans.approval.ServiceNowApprovalParams;
import software.wings.beans.approval.ShellScriptApprovalParams;
import software.wings.beans.approval.SlackApprovalParams;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.command.Command.Builder;
import software.wings.beans.command.CommandType;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.security.SecretManager;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.impl.notifications.SlackApprovalMessageKeys;
import software.wings.service.impl.workflow.WorkflowNotificationHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.ApprovalPolingService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.servicenow.ServiceNowService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.State;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.EnvState.EnvStateKeys;
import software.wings.sm.states.mixin.SweepingOutputStateMixin;
import software.wings.utils.Misc;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

@Slf4j
@FieldNameConstants(innerTypeName = "ApprovalStateKeys")
public class ApprovalState extends State implements SweepingOutputStateMixin {
  public static final String APPROVAL_STATUS_KEY = "approvalStatus";

  @Getter @Setter private String groupName;
  @Getter @Setter private List<String> userGroups = new ArrayList<>();
  @Getter @Setter private boolean disable;
  @Getter @Setter private String disableAssertion;

  public enum ApprovalStateType { JIRA, USER_GROUP, SHELL_SCRIPT, SERVICENOW }
  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  @Inject private AlertService alertService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject private NotificationService notificationService;
  @Inject private JiraHelperService jiraHelperService;
  @Inject private ServiceNowService serviceNowService;
  @Inject private transient ActivityService activityService;
  @Inject private transient WorkflowNotificationHelper workflowNotificationHelper;
  @Inject private ApprovalPolingService approvalPolingService;
  @Inject private SecretManager secretManager;
  @Inject private PipelineService pipelineService;
  @Inject private transient SweepingOutputService sweepingOutputService;
  @Inject private UserGroupService userGroupService;

  @Inject @Named("ServiceJobScheduler") private PersistentScheduler serviceJobScheduler;
  private Integer DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS = 7 * 24 * 60 * 60 * 1000; // 7 days
  private static int MAX_TIMEOUT_MINUTES = 30000;
  private static int MAX_TIMEOUT_MILLIS = MAX_TIMEOUT_MINUTES * 60 * 1000;
  private String SCRIPT_APPROVAL_COMMAND = "Execute Approval Script";
  private String SCRIPT_APPROVAL_JOB_GROUP = "SHELL_SCRIPT_APPROVAL_JOB";
  public static final String APPROVAL_STATE_TYPE_VARIABLE = "approvalStateType";
  public static final String USER_GROUPS_VARIABLE = "userGroups";

  @Getter @Setter ApprovalStateParams approvalStateParams;
  @Getter @Setter ApprovalStateType approvalStateType = USER_GROUP;

  @Getter @Setter private SweepingOutputInstance.Scope sweepingOutputScope = Scope.PIPELINE;
  @Getter @Setter private String sweepingOutputName;

  @Getter @Setter List<NameValuePair> variables;
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
                                                   .variables(getVariables())
                                                   .build();
    if (disableAssertion != null && disableAssertion.equals("true")) {
      return respondWithStatus(context, executionData, null,
          ExecutionResponse.builder()
              .executionStatus(SKIPPED)
              .errorMessage(getName() + " step in " + context.getPipelineStageName() + " has been skipped")
              .stateExecutionData(executionData));
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

    if (approvalStateType == null) {
      approvalStateType = USER_GROUP;
    }
    try {
      workflowNotificationHelper.sendApprovalNotification(
          app.getAccountId(), APPROVAL_NEEDED_NOTIFICATION, placeholderValues, context, approvalStateType);
    } catch (Exception e) {
      // catch exception so that failure to send notification doesn't affect rest of execution
      logger.error("Error sending approval notification. accountId={}", app.getAccountId(), e);
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
    executionData.setApprovalStateType(approvalStateType);

    switch (approvalStateType) {
      case JIRA:
        return executeJiraApproval(context, executionData, approvalId);
      case SERVICENOW:
        return executeServiceNowApproval(context, executionData, approvalId);
      case USER_GROUP:
        return executeUserGroupApproval(
            userGroups, app.getAccountId(), placeholderValues, approvalId, executionData, app.getUuid(), context);
      case SHELL_SCRIPT:
        return executeShellScriptApproval(context, app.getAccountId(), app.getUuid(), approvalId,
            approvalStateParams.getShellScriptApprovalParams(), executionData);
      default:
        throw new InvalidRequestException(
            "Invalid ApprovalStateType, it should be one of ServiceNow, Jira, HarnessUi or Custom Shell script");
    }
  }

  @Override
  public void parseProperties(Map<String, Object> properties) {
    prepareVariables(properties);
    boolean isDisabled = properties.get(EnvStateKeys.disable) != null && (boolean) properties.get(EnvStateKeys.disable);
    if (isDisabled && properties.get(EnvStateKeys.disableAssertion) == null) {
      properties.put(EnvStateKeys.disableAssertion, "true");
    }
    super.parseProperties(properties);
  }

  /*
  1) Remove duplicate variable names
  2) Replace null by empty String
   */
  private void prepareVariables(Map<String, Object> properties) {
    List<Map<String, String>> variableMapList = (List<Map<String, String>>) properties.get(ApprovalStateKeys.variables);
    Set<String> duplicateTracker = new HashSet<>();
    if (variableMapList != null) {
      Iterator<Map<String, String>> iterator = variableMapList.iterator();
      while (iterator.hasNext()) {
        Map<String, String> variableMap = iterator.next();
        String variableName = variableMap.get(NameValuePairKeys.name);
        if (!duplicateTracker.add(variableName)) {
          iterator.remove();
        }
        variableMap.putIfAbsent(NameValuePairKeys.value, StringUtils.EMPTY);
      }
    }
  }

  @VisibleForTesting
  void setPipelineVariables(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Map<String, Object> workflowVariables = new HashMap<>();
    if (isNotEmpty(workflowStandardParams.getWorkflowVariables())) {
      workflowStandardParams.getWorkflowVariables().forEach(workflowVariables::put);
      if (isNotEmpty(workflowVariables)) {
        if (workflowStandardParams.getWorkflowElement() == null) {
          workflowStandardParams.setWorkflowElement(WorkflowElement.builder().variables(workflowVariables).build());
        } else {
          workflowStandardParams.getWorkflowElement().setVariables(workflowVariables);
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
        && executionContext.getOrchestrationWorkflowType() == BUILD) {
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
      return respondWithStatus(context, executionData, null,
          ExecutionResponse.builder()
              .async(true)
              .executionStatus(PAUSED)
              .errorMessage("Waiting for Approval")
              .correlationIds(singletonList(approvalId))
              .stateExecutionData(executionData));
    } catch (WingsException e) {
      return respondWithStatus(context, executionData, null,
          ExecutionResponse.builder()
              .executionStatus(FAILED)
              .errorMessage("Failed to schedule Approval" + e.getMessage())
              .stateExecutionData(executionData));
    }
  }

  @VisibleForTesting
  ExecutionResponse executeJiraApproval(
      ExecutionContext context, ApprovalStateExecutionData executionData, String approvalId) {
    JiraApprovalParams jiraApprovalParams = approvalStateParams.getJiraApprovalParams();
    jiraApprovalParams.setIssueId(context.renderExpression(jiraApprovalParams.getIssueId()));

    if (ExpressionEvaluator.containsVariablePattern(jiraApprovalParams.getIssueId())) {
      return respondWithStatus(context, executionData, null,
          ExecutionResponse.builder()
              .executionStatus(FAILED)
              .errorMessage("Expression not rendered for Jira issue Id: " + jiraApprovalParams.getIssueId())
              .stateExecutionData(executionData));
    }

    Application app = context.getApp();

    executionData.setApprovalField(jiraApprovalParams.getApprovalField());
    executionData.setApprovalValue(jiraApprovalParams.getApprovalValue());
    executionData.setRejectionField(jiraApprovalParams.getRejectionField());
    executionData.setRejectionValue(jiraApprovalParams.getRejectionValue());

    JiraExecutionData jiraExecutionData = jiraHelperService.fetchIssue(
        jiraApprovalParams, app.getAccountId(), app.getAppId(), context.getWorkflowExecutionId(), approvalId);

    if (jiraExecutionData.getExecutionStatus() != null && FAILED == jiraExecutionData.getExecutionStatus()) {
      return respondWithStatus(context, executionData, null,
          ExecutionResponse.builder()
              .executionStatus(FAILED)
              .errorMessage(jiraExecutionData.getErrorMessage())
              .stateExecutionData(executionData));
    }

    executionData.setIssueUrl(jiraExecutionData.getIssueUrl());
    executionData.setIssueKey(jiraExecutionData.getIssueKey());
    executionData.setCurrentStatus(jiraExecutionData.getCurrentStatus());

    if (jiraExecutionData.getCurrentStatus().equalsIgnoreCase(jiraApprovalParams.getApprovalValue())) {
      return respondWithStatus(context, executionData, null,
          ExecutionResponse.builder()
              .executionStatus(SUCCESS)
              .errorMessage("Approval provided on ticket: " + jiraExecutionData.getIssueKey())
              .stateExecutionData(executionData));
    }

    if (jiraApprovalParams.getRejectionValue() != null
        && jiraExecutionData.getCurrentStatus().equalsIgnoreCase(jiraApprovalParams.getRejectionValue())) {
      return respondWithStatus(context, executionData, null,
          ExecutionResponse.builder()
              .executionStatus(REJECTED)
              .errorMessage("Rejection provided on ticket: " + jiraExecutionData.getIssueKey())
              .stateExecutionData(executionData));
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
      return respondWithStatus(context, executionData, null,
          ExecutionResponse.builder()
              .async(true)
              .executionStatus(PAUSED)
              .errorMessage(jiraExecutionData.getErrorMessage())
              .correlationIds(asList(approvalId))
              .stateExecutionData(executionData));
    } catch (WingsException e) {
      return respondWithStatus(context, executionData, null,
          ExecutionResponse.builder()
              .executionStatus(FAILED)
              .errorMessage("Failed to schedule Approval" + e.getMessage())
              .stateExecutionData(executionData));
    }
  }

  @VisibleForTesting
  ExecutionResponse executeServiceNowApproval(
      ExecutionContext context, ApprovalStateExecutionData executionData, String approvalId) {
    ServiceNowApprovalParams servicenowApprovalParams = approvalStateParams.getServiceNowApprovalParams();
    servicenowApprovalParams.setIssueNumber(context.renderExpression(servicenowApprovalParams.getIssueNumber()));

    if (ExpressionEvaluator.containsVariablePattern(servicenowApprovalParams.getIssueNumber())) {
      return respondWithStatus(context, executionData, null,
          ExecutionResponse.builder()
              .executionStatus(FAILED)
              .errorMessage("Expression not rendered for issue Number: " + servicenowApprovalParams.getIssueNumber())
              .stateExecutionData(executionData));
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
        return respondWithStatus(context, executionData, null,
            ExecutionResponse.builder()
                .executionStatus(SUCCESS)
                .errorMessage("Approval provided on ticket: " + servicenowApprovalParams.getIssueNumber())
                .stateExecutionData(executionData));
      }

      if (servicenowApprovalParams.getRejectionValue() != null
          && serviceNowExecutionData.getCurrentState().equalsIgnoreCase(servicenowApprovalParams.getRejectionValue())) {
        return respondWithStatus(context, executionData, null,
            ExecutionResponse.builder()
                .executionStatus(REJECTED)
                .errorMessage("Rejection provided on ticket: " + servicenowApprovalParams.getIssueNumber())
                .stateExecutionData(executionData));
      }

    } catch (WingsException we) {
      return respondWithStatus(context, executionData, null,
          ExecutionResponse.builder()
              .executionStatus(FAILED)
              .errorMessage(we.getParams().get("message").toString())
              .stateExecutionData(executionData));
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
      return respondWithStatus(context, executionData, null,
          ExecutionResponse.builder()
              .async(true)
              .executionStatus(PAUSED)
              .errorMessage("Waiting for approval on Ticket " + servicenowApprovalParams.getIssueNumber())
              .correlationIds(asList(approvalId))
              .stateExecutionData(executionData));
    } catch (WingsException e) {
      return respondWithStatus(context, executionData, null,
          ExecutionResponse.builder()
              .executionStatus(FAILED)
              .errorMessage("Failed to schedule Approval" + e.getMessage())
              .stateExecutionData(executionData));
    }
  }

  private ExecutionResponse executeUserGroupApproval(List<String> userGroups, String accountId,
      Map<String, String> placeholderValues, String approvalId, ApprovalStateExecutionData executionData, String appId,
      ExecutionContext context) {
    executionData.setUserGroups(userGroups);
    updatePlaceholderValuesForSlackApproval(approvalId, accountId, placeholderValues, context);
    sendNotificationForUserGroupApproval(userGroups, appId, accountId, APPROVAL_NEEDED_NOTIFICATION, placeholderValues);

    return respondWithStatus(context, executionData, null,
        ExecutionResponse.builder()
            .async(true)
            .executionStatus(PAUSED)
            .correlationIds(asList(approvalId))
            .stateExecutionData(executionData));
  }

  private void updatePlaceholderValuesForSlackApproval(
      String approvalId, String accountId, Map<String, String> placeHolderValues, ExecutionContext context) {
    String pausedStageName = null;
    StringJoiner environments = new StringJoiner(", ");
    StringJoiner services = new StringJoiner(", ");
    StringJoiner artifacts = new StringJoiner(", ");

    int tokenValidDuration = getTimeoutMillis();
    boolean isPipeline = context.getWorkflowType() == WorkflowType.PIPELINE;
    if (isPipeline) {
      Pipeline pipeline = pipelineService.readPipeline(context.getAppId(), context.getWorkflowId(), true);
      pausedStageName = getPipelineStageName(pipeline, getName());
    } else {
      pausedStageName = context.getStateExecutionInstanceName();
    }

    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetails(context.getAppId(), context.getWorkflowExecutionId(), true);
    if (EmptyPredicate.isNotEmpty(workflowExecution.getExecutionArgs().getArtifacts())) {
      for (Artifact artifact : workflowExecution.getExecutionArgs().getArtifacts()) {
        artifacts.add(
            artifact.getArtifactSourceName() + ": " + artifact.getMetadata().get(ArtifactMetadataKeys.buildNo));
      }
    }
    if (EmptyPredicate.isNotEmpty(workflowExecution.getEnvironments())) {
      for (EnvSummary envSummary : workflowExecution.getEnvironments()) {
        environments.add(envSummary.getName());
      }
    }
    if (EmptyPredicate.isNotEmpty(workflowExecution.getServiceExecutionSummaries())) {
      for (ElementExecutionSummary elementExecutionSummary : workflowExecution.getServiceExecutionSummaries()) {
        services.add(elementExecutionSummary.getContextElement().getName());
      }
    }

    Map<String, String> claims = new HashMap<>();
    claims.put("approvalId", approvalId);
    String workflowURL = placeHolderValues.get("WORKFLOW_URL");

    String jwtToken = secretManager.generateJWTTokenWithCustomTimeOut(
        claims, secretManager.getJWTSecret(EXTERNAL_SERVICE_SECRET), tokenValidDuration);

    SlackApprovalParams slackApprovalParams = SlackApprovalParams.builder()
                                                  .appId(context.getAppId())
                                                  .appName(context.getApp().getName())
                                                  .routingId(accountId)
                                                  .deploymentId(context.getWorkflowExecutionId())
                                                  .workflowId(context.getWorkflowId())
                                                  .workflowExecutionName(context.getWorkflowExecutionName())
                                                  .stateExecutionId(context.getStateExecutionInstanceId())
                                                  .stateExecutionInstanceName(context.getStateExecutionInstanceName())
                                                  .approvalId(approvalId)
                                                  .pausedStageName(pausedStageName)
                                                  .servicesInvolved(services.toString())
                                                  .environmentsInvolved(environments.toString())
                                                  .artifactsInvolved(artifacts.toString())
                                                  .confirmation(false)
                                                  .pipeline(isPipeline)
                                                  .workflowUrl(workflowURL)
                                                  .jwtToken(jwtToken)
                                                  .build();
    JSONObject customData = new JSONObject(slackApprovalParams);

    URL notificationTemplateUrl;
    if (slackApprovalParams.isPipeline()) {
      notificationTemplateUrl =
          this.getClass().getResource(SlackApprovalMessageKeys.PIPELINE_APPROVAL_MESSAGE_TEMPLATE);
    } else {
      notificationTemplateUrl =
          this.getClass().getResource(SlackApprovalMessageKeys.WORKFLOW_APPROVAL_MESSAGE_TEMPLATE);
    }

    String displayText = createSlackApprovalMessage(slackApprovalParams, notificationTemplateUrl);
    String buttonValue = customData.toString();
    buttonValue = StringEscapeUtils.escapeJson(buttonValue);
    placeHolderValues.put(SlackApprovalMessageKeys.SLACK_APPROVAL_PARAMS, buttonValue);
    placeHolderValues.put(SlackApprovalMessageKeys.APPROVAL_MESSAGE, displayText);
    placeHolderValues.put(SlackApprovalMessageKeys.MESSAGE_IDENTIFIER, "suppressTraditionalNotificationOnSlack");
  }

  private String getPipelineStageName(Pipeline pipeline, String pipelineApprovalStageName) {
    List<PipelineStage> pipelineStages = pipeline.getPipelineStages();
    for (PipelineStage pipelineStage : pipelineStages) {
      List<PipelineStageElement> pipelineStageElements = pipelineStage.getPipelineStageElements();
      for (PipelineStageElement pipelineStageElement : pipelineStageElements) {
        if (pipelineStageElement.getName().equals(pipelineApprovalStageName)) {
          return pipelineStage.getName() == null ? SlackApprovalMessageKeys.PAUSED_STAGE_NAME_DEFAULT
                                                 : pipelineStage.getName();
        }
      }
    }
    return null;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ApprovalStateExecutionData approvalNotifyResponse =
        (ApprovalStateExecutionData) response.values().iterator().next();

    boolean isApprovalFromSlack = approvalNotifyResponse.isApprovalFromSlack();
    if (isNotEmpty(approvalNotifyResponse.getVariables())) {
      setVariables(approvalNotifyResponse.getVariables());
    }

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

    if (approvalStateType == null) {
      approvalStateType = USER_GROUP;
    }
    workflowNotificationHelper.sendApprovalNotification(
        app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues, context, approvalStateType);

    executionData.setVariables(approvalNotifyResponse.getVariables());

    switch (approvalStateType) {
      case JIRA:
        return handleAsyncJira(context, executionData, approvalNotifyResponse);
      case SERVICENOW:
        return handleAsyncServiceNow(context, executionData, approvalNotifyResponse);
      case USER_GROUP:
        return handleAsyncUserGroup(
            userGroups, placeholderValues, context, executionData, approvalNotifyResponse, isApprovalFromSlack);
      case SHELL_SCRIPT:
        return handleAsyncShellScript(context, executionData, approvalNotifyResponse);
      default:
        throw new InvalidRequestException("Invalid ApprovalStateType");
    }
  }

  void fillSweepingOutput(ExecutionContext context, ApprovalStateExecutionData executionData,
      ApprovalStateExecutionData approvalNotifyResponse) {
    Map<String, Object> output = new HashMap<>();
    Map<String, Object> variableMap = new HashMap<>();
    if (isNotEmpty(variables)) {
      for (NameValuePair expression : variables) {
        variableMap.put(expression.getName(),
            context.renderExpression(
                expression.getValue(), StateExecutionContext.builder().stateExecutionData(executionData).build()));
        // We would want to deprecate use of directly accessing variables, once that is done,
        // this block of code can be removed
        output.put(expression.getName(),
            context.renderExpression(
                expression.getValue(), StateExecutionContext.builder().stateExecutionData(executionData).build()));
      }
    }

    output.put(ApprovalStateExecutionDataKeys.variables, variableMap);
    output.put(ApprovalStateExecutionDataKeys.approvalStateType, executionData.getApprovalStateType());
    output.put(ApprovalStateExecutionDataKeys.timeoutMillis, executionData.getTimeoutMillis());
    output.put(APPROVAL_STATUS_KEY,
        approvalNotifyResponse != null ? StringUtils.capitalize(String.valueOf(approvalNotifyResponse.getStatus()))
                                       : null);
    switch (executionData.getApprovalStateType()) {
      case USER_GROUP:
        output.put(ApprovalStateExecutionDataKeys.approvedBy, executionData.getApprovedBy());
        output.put(ApprovalStateExecutionDataKeys.approvedOn, executionData.getApprovedOn());
        output.put(ApprovalStateExecutionDataKeys.comments, executionData.getComments());
        if (isNotEmpty(executionData.getUserGroups())) {
          output.put(ApprovalStateExecutionDataKeys.userGroups,
              userGroupService.fetchUserGroupNamesFromIds(executionData.getUserGroups()));
        }
        break;
      case JIRA:
        output.put(ApprovalStateExecutionDataKeys.issueUrl, executionData.getIssueUrl());
        output.put(ApprovalStateExecutionDataKeys.issueKey, executionData.getIssueKey());
        output.put(ApprovalStateExecutionDataKeys.currentStatus, executionData.getCurrentStatus());
        output.put(ApprovalStateExecutionDataKeys.approvalField, executionData.getApprovalField());
        output.put(ApprovalStateExecutionDataKeys.approvalValue, executionData.getApprovalValue());
        output.put(ApprovalStateExecutionDataKeys.rejectionField, executionData.getRejectionField());
        output.put(ApprovalStateExecutionDataKeys.rejectionValue, executionData.getRejectionValue());
        break;
      case SERVICENOW:
        output.put(ApprovalStateExecutionDataKeys.ticketUrl, executionData.getTicketUrl());
        output.put(ApprovalStateExecutionDataKeys.ticketType, executionData.getTicketType());
        break;
      case SHELL_SCRIPT:
        break;
      default:
        logger.warn("Unsupported approval type ", executionData.getApprovalStateType());
    }

    // Slack Approval
    output.put(ApprovalStateExecutionDataKeys.approvalFromSlack, executionData.isApprovalFromSlack());

    handleSweepingOutput(sweepingOutputService, context, output);
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

    return respondWithStatus(context, executionData, approvalNotifyResponse,
        ExecutionResponse.builder()
            .stateExecutionData(executionData)
            .executionStatus(approvalNotifyResponse.getStatus())
            .errorMessage(errorMessage));
  }

  private ExecutionResponse respondWithStatus(ExecutionContext context, ApprovalStateExecutionData executionData,
      ApprovalStateExecutionData approvalNotifyResponse, ExecutionResponseBuilder executionResponseBuilder) {
    ExecutionResponse executionResponse = executionResponseBuilder.build();
    if (ExecutionStatus.isFinalStatus(executionResponse.getExecutionStatus())) {
      fillSweepingOutput(context, executionData, approvalNotifyResponse);
    }
    return executionResponse;
  }

  private ExecutionResponse handleAsyncJira(ExecutionContext context, ApprovalStateExecutionData executionData,
      ApprovalStateExecutionData approvalNotifyResponse) {
    JiraApprovalParams jiraApprovalParams = approvalStateParams.getJiraApprovalParams();
    setPipelineVariables(context);
    jiraApprovalParams.setIssueId(context.renderExpression(jiraApprovalParams.getIssueId()));
    logger.info("Deleting job for approvalId: {}, workflowExecutionId: {} ", executionData.getApprovalId(),
        executionData.getWorkflowId());
    approvalPolingService.delete(executionData.getApprovalId());

    return respondWithStatus(context, executionData, approvalNotifyResponse,
        ExecutionResponse.builder()
            .stateExecutionData(executionData)
            .executionStatus(approvalNotifyResponse.getStatus())
            .errorMessage(approvalNotifyResponse.getErrorMsg() + executionData.getIssueKey()));
  }

  private ExecutionResponse handleAsyncServiceNow(ExecutionContext context, ApprovalStateExecutionData executionData,
      ApprovalStateExecutionData approvalNotifyResponse) {
    ServiceNowApprovalParams servicenowApprovalParams = approvalStateParams.getServiceNowApprovalParams();
    servicenowApprovalParams.setIssueNumber(context.renderExpression(servicenowApprovalParams.getIssueNumber()));

    logger.info("Deleting job for approvalId: {}, workflowExecutionId: {} ", executionData.getApprovalId(),
        executionData.getWorkflowId());
    approvalPolingService.delete(executionData.getApprovalId());

    setPipelineVariables(context);
    return respondWithStatus(context, executionData, approvalNotifyResponse,
        ExecutionResponse.builder()
            .stateExecutionData(executionData)
            .executionStatus(approvalNotifyResponse.getStatus())
            .errorMessage(approvalNotifyResponse.getErrorMsg() + servicenowApprovalParams.getIssueNumber()));
  }

  private ExecutionResponse handleAsyncUserGroup(List<String> userGroups, Map<String, String> placeholderValues,
      ExecutionContext context, ApprovalStateExecutionData executionData,
      ApprovalStateExecutionData approvalNotifyResponse, boolean isApprovalFromSlack) {
    Application app = context.getApp();
    if (!isApprovalFromSlack) {
      sendNotificationForUserGroupApproval(
          userGroups, app.getUuid(), app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues);
    }

    return respondWithStatus(context, executionData, approvalNotifyResponse,
        ExecutionResponse.builder()
            .stateExecutionData(executionData)
            .executionStatus(approvalNotifyResponse.getStatus()));
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
    if (approvalStateType == null) {
      approvalStateType = USER_GROUP;
    }
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
          app.getAccountId(), notificationMessageType, placeholderValues, context, approvalStateType);
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
          app.getAccountId(), notificationMessageType, placeholderValues, context, approvalStateType);
    }

    context.getStateExecutionData().setErrorMsg(errorMsg);

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
        throw new InvalidRequestException("Invalid ApprovalStateType : neither JIRA nor USER_GROUP");
    }
  }

  private void sendNotificationForUserGroupApproval(List<String> approvalUserGroups, String appId, String accountId,
      NotificationMessageType notificationMessageType, Map<String, String> placeHolderValues) {
    NotificationRule rule = NotificationRuleBuilder.aNotificationRule().withUserGroupIds(approvalUserGroups).build();
    InformationNotification notification = InformationNotification.builder()
                                               .appId(appId)
                                               .accountId(accountId)
                                               .notificationTemplateId(notificationMessageType.name())
                                               .notificationTemplateVariables(placeHolderValues)
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
  }

  private void handleAbortEventServiceNow(ExecutionContext context) {
    ApprovalStateExecutionData executionData = (ApprovalStateExecutionData) context.getStateExecutionData();
    approvalPolingService.delete(executionData.getApprovalId());
  }

  @Override
  public Integer getTimeoutMillis() {
    if (super.getTimeoutMillis() == null) {
      return DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS;
    }
    return super.getTimeoutMillis();
  }

  /*
  To validate property map before state Object is constructed. This can prevent
  stateTransformationErrors.
   */
  public static void preValidatePropertyMap(Map<String, Object> propertyMap) {
    final double timeoutMillis = Double.valueOf(propertyMap.getOrDefault(StateKeys.timeoutMillis, 0).toString());
    if (timeoutMillis > MAX_TIMEOUT_MILLIS) {
      throw new InvalidRequestException(format("Timeout value cannot be greater than %s minutes", MAX_TIMEOUT_MINUTES));
    }
  }

  public static String fetchAndTrimSweepingOutputName(Map<String, Object> propertyMap) {
    String publishedVarName = propertyMap.getOrDefault(ApprovalStateKeys.sweepingOutputName, "").toString().trim();
    if (isNotEmpty(publishedVarName)) {
      propertyMap.put(ApprovalStateKeys.sweepingOutputName, publishedVarName);
    }
    return publishedVarName;
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
    Application app = Objects.requireNonNull(context.getApp());
    WorkflowExecution workflowExecution =
        workflowExecutionService.getWorkflowExecution(app.getUuid(), context.getWorkflowExecutionId());

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
