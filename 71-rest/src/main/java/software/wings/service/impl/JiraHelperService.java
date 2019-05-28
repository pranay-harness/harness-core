package software.wings.service.impl;

import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.USER;
import static software.wings.delegatetasks.jira.JiraAction.CHECK_APPROVAL;
import static software.wings.delegatetasks.jira.JiraAction.FETCH_ISSUE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.JiraExecutionData;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.JiraConfig;
import software.wings.beans.TaskType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.approval.ApprovalPollingJobEntity;
import software.wings.beans.approval.JiraApprovalParams;
import software.wings.beans.jira.JiraTaskParameters;
import software.wings.delegatetasks.jira.JiraAction;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.states.ApprovalState;

/**
 * All Jira apis should be accessed via this object.
 */
@Singleton
@Slf4j
public class JiraHelperService {
  private static final String WORKFLOW_EXECUTION_ID = "workflow";
  private static final long JIRA_DELEGATE_TIMEOUT_MILLIS = 60 * 1000;
  @Inject private DelegateServiceImpl delegateService;
  @Inject @Transient private transient SecretManager secretManager;
  @Inject SettingsService settingService;
  @Inject WorkflowExecutionService workflowExecutionService;
  @Inject WaitNotifyEngine waitNotifyEngine;
  @Inject private MainConfiguration mainConfiguration;
  @Inject StateExecutionService stateExecutionService;

  private static final String JIRA_APPROVAL_API_PATH = "api/ticketing/jira-approval/";
  public static final String APP_ID_KEY = "app_id";
  public static final String WORKFLOW_EXECUTION_ID_KEY = "workflow_execution_id";
  public static final String APPROVAL_FIELD_KEY = "approval_field";
  public static final String APPROVAL_VALUE_KEY = "approval_value";
  public static final String REJECTION_FIELD_KEY = "rejection_field";
  public static final String REJECTION_VALUE_KEY = "rejection_value";
  public static final String APPROVAL_ID_KEY = "approval_id";
  @Inject private software.wings.security.SecretManager secretManagerForToken;

  /**
   * Validate credential.
   */
  public void validateCredential(JiraConfig jiraConfig) {
    String accountId = jiraConfig.getAccountId();
    JiraTaskParameters jiraTaskParameters =
        JiraTaskParameters.builder().accountId(accountId).jiraAction(JiraAction.AUTH).build();

    jiraTaskParameters.setJiraConfig(jiraConfig);
    jiraTaskParameters.setEncryptionDetails(
        secretManager.getEncryptionDetails(jiraConfig, APP_ID_KEY, WORKFLOW_EXECUTION_ID));

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(accountId)
                                    .appId(APP_ID_KEY)
                                    .data(TaskData.builder()
                                              .taskType(TaskType.JIRA.name())
                                              .parameters(new Object[] {jiraTaskParameters})
                                              .timeout(JIRA_DELEGATE_TIMEOUT_MILLIS)
                                              .build())
                                    .async(false)
                                    .build();

    JiraExecutionData jiraExecutionData;
    try {
      jiraExecutionData = delegateService.executeTask(delegateTask);
    } catch (Exception e) {
      throw new WingsException("Unexpected error during authentication to JIRA server");
    }

    if (jiraExecutionData.getExecutionStatus() != ExecutionStatus.SUCCESS) {
      throw new WingsException("Failed to Authenticate with JIRA Server.");
    }
  }

  public void handleJiraPolling(ApprovalPollingJobEntity entity) {
    JiraExecutionData jiraExecutionData = null;
    String issueId = entity.getIssueId();
    String approvalId = entity.getApprovalId();
    String workflowExecutionId = entity.getWorkflowExecutionId();
    String appId = entity.getAppId();
    String stateExecutionInstanceId = entity.getStateExecutionInstanceId();
    try {
      jiraExecutionData = getApprovalStatus(entity);
    } catch (Exception ex) {
      logger.warn(
          "Error occurred while polling JIRA status. Continuing to poll next minute. approvalId: {}, workflowExecutionId: {} , issueId: {}",
          entity.getApprovalId(), entity.getWorkflowExecutionId(), entity.getIssueId(), ex);
      return;
    }

    ExecutionStatus issueStatus = jiraExecutionData.getExecutionStatus();
    logger.info("Issue: {} Status from JIRA: {} Current Status {} for approvalId: {}, workflowExecutionId: {} ",
        issueId, issueStatus, jiraExecutionData.getCurrentStatus(), approvalId, workflowExecutionId);

    try {
      if (issueStatus == ExecutionStatus.SUCCESS || issueStatus == ExecutionStatus.REJECTED) {
        ApprovalDetails.Action action =
            issueStatus == ExecutionStatus.SUCCESS ? ApprovalDetails.Action.APPROVE : ApprovalDetails.Action.REJECT;

        approveWorkflow(action, approvalId, appId, workflowExecutionId, issueStatus,
            jiraExecutionData.getCurrentStatus(), stateExecutionInstanceId);
      } else if (issueStatus == ExecutionStatus.PAUSED) {
        logger.info("Still waiting for approval or rejected for issueId {}. Issue Status {} and Current Status {}",
            issueId, issueStatus, jiraExecutionData.getCurrentStatus());
        continuePauseWorkflow(approvalId, appId, workflowExecutionId, issueStatus, jiraExecutionData.getCurrentStatus(),
            stateExecutionInstanceId);
      } else if (issueStatus == ExecutionStatus.FAILED) {
        logger.info("Jira delegate task failed with error: " + jiraExecutionData.getErrorMessage());
      }
    } catch (WingsException exception) {
      exception.addContext(Application.class, appId);
      exception.addContext(WorkflowExecution.class, workflowExecutionId);
      exception.addContext(ApprovalState.class, approvalId);
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.warn("Error while getting execution data, approvalId: {}, workflowExecutionId: {} , issueId: {}",
          approvalId, workflowExecutionId, issueId, exception);
    }
  }

  public JSONArray getProjects(String connectorId, String accountId, String appId) {
    JiraTaskParameters jiraTaskParameters =
        JiraTaskParameters.builder().accountId(accountId).jiraAction(JiraAction.GET_PROJECTS).build();

    JiraExecutionData jiraExecutionData = runTask(accountId, appId, connectorId, jiraTaskParameters);
    if (jiraExecutionData.getExecutionStatus() != ExecutionStatus.SUCCESS) {
      throw new WingsException("Failed to fetch Projects");
    }
    return jiraExecutionData.getProjects();
  }

  public JiraExecutionData fetchIssue(JiraApprovalParams jiraApprovalParams, String accountId, String appId,
      String workflowExecutionId, String approvalId) {
    JiraTaskParameters jiraTaskParameters = JiraTaskParameters.builder()
                                                .accountId(accountId)
                                                .appId(appId)
                                                .approvalId(approvalId)
                                                .approvalField(jiraApprovalParams.getApprovalField())
                                                .jiraAction(FETCH_ISSUE)
                                                .issueId(jiraApprovalParams.getIssueId())
                                                .build();
    return runTask(accountId, appId, jiraApprovalParams.getJiraConnectorId(), jiraTaskParameters);
  }

  /**
   * Fetch list of fields and list of value options for each field.
   *
   * @param connectorId
   * @param project
   * @param accountId
   * @param appId
   * @return
   */
  public Object getFieldOptions(String connectorId, String project, String accountId, String appId) {
    JiraTaskParameters jiraTaskParameters = JiraTaskParameters.builder()
                                                .accountId(accountId)
                                                .jiraAction(JiraAction.GET_FIELDS_OPTIONS)
                                                .project(project)
                                                .build();

    JiraExecutionData jiraExecutionData = runTask(accountId, appId, connectorId, jiraTaskParameters);
    if (jiraExecutionData.getExecutionStatus() != ExecutionStatus.SUCCESS) {
      throw new WingsException("Failed to fetch IssueType and Priorities");
    }

    return jiraExecutionData.getFields();
  }

  public Object getStatuses(String connectorId, String project, String accountId, String appId) {
    JiraTaskParameters jiraTaskParameters =
        JiraTaskParameters.builder().accountId(accountId).jiraAction(JiraAction.GET_STATUSES).project(project).build();

    JiraExecutionData jiraExecutionData = runTask(accountId, appId, connectorId, jiraTaskParameters);
    if (jiraExecutionData.getExecutionStatus() != ExecutionStatus.SUCCESS) {
      throw new WingsException("Failed to fetch Status for this project");
    }
    return jiraExecutionData.getStatuses();
  }

  private String getBaseUrl() {
    String baseUrl = mainConfiguration.getPortal().getUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return baseUrl;
  }

  private JiraExecutionData runTask(
      String accountId, String appId, String connectorId, JiraTaskParameters jiraTaskParameters) {
    JiraConfig jiraConfig = (JiraConfig) settingService.get(connectorId).getValue();
    jiraTaskParameters.setJiraConfig(jiraConfig);
    jiraTaskParameters.setEncryptionDetails(
        secretManager.getEncryptionDetails(jiraConfig, appId, WORKFLOW_EXECUTION_ID));

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(accountId)
                                    .appId(appId)
                                    .data(TaskData.builder()
                                              .taskType(TaskType.JIRA.name())
                                              .parameters(new Object[] {jiraTaskParameters})
                                              .timeout(JIRA_DELEGATE_TIMEOUT_MILLIS)
                                              .build())
                                    .async(false)
                                    .build();

    ResponseData responseData = delegateService.executeTask(delegateTask);
    if (jiraTaskParameters.getJiraAction().equals(CHECK_APPROVAL) && delegateTask != null) {
      logger.info("Delegate task Id = {}, for Polling Jira Approval for IssueId {}", delegateTask.getUuid(),
          jiraTaskParameters.getIssueId());
    }
    if (responseData instanceof JiraExecutionData) {
      return (JiraExecutionData) responseData;
    } else {
      return JiraExecutionData.builder().errorMessage("Delegate task failed with an exception").build();
    }
  }

  public Object getCreateMetadata(String connectorId, String expand, String project, String accountId, String appId) {
    JiraTaskParameters jiraTaskParameters = JiraTaskParameters.builder()
                                                .accountId(accountId)
                                                .jiraAction(JiraAction.GET_CREATE_METADATA)
                                                .createmetaExpandParam(expand)
                                                .project(project)
                                                .build();

    JiraExecutionData jiraExecutionData = runTask(accountId, appId, connectorId, jiraTaskParameters);
    if (jiraExecutionData.getExecutionStatus() != ExecutionStatus.SUCCESS) {
      throw new WingsException("Failed to fetch Projects and Issue Metadata");
    }
    return jiraExecutionData.getCreateMetadata();
  }

  public void approveWorkflow(Action action, String approvalId, String appId, String workflowExecutionId,
      ExecutionStatus approvalStatus, String currentStatus, String stateExecutionInstanceId) {
    ApprovalStateExecutionData executionData;
    if (stateExecutionInstanceId == null) {
      ApprovalDetails approvalDetails = new ApprovalDetails();
      approvalDetails.setAction(action);
      approvalDetails.setApprovalId(approvalId);
      executionData = workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
          appId, workflowExecutionId, null, approvalDetails);
    } else {
      StateExecutionInstance stateExecutionInstance =
          stateExecutionService.getStateExecutionData(appId, stateExecutionInstanceId);
      if (stateExecutionInstance == null) {
        throw new WingsException(INVALID_ARGUMENT, USER)
            .addParam("args", "No stateExecutionInstace found for id " + stateExecutionInstanceId);
      }
      executionData = (ApprovalStateExecutionData) stateExecutionInstance.getStateExecutionMap().get(
          stateExecutionInstance.getDisplayName());
    }
    String message;
    if (action == Action.APPROVE) {
      message = "Approval provided on ticket: " + executionData.getIssueKey();
    } else {
      message = "Rejection provided on ticket: " + executionData.getIssueKey();
    }

    executionData.setStatus(approvalStatus);
    executionData.setApprovedOn(System.currentTimeMillis());
    executionData.setCurrentStatus(currentStatus);

    logger.info("Sending notify for approvalId: {}, workflowExecutionId: {} ", approvalId, workflowExecutionId);
    waitNotifyEngine.notify(approvalId, executionData);
  }

  public void continuePauseWorkflow(String approvalId, String appId, String workflowExecutionId,
      ExecutionStatus approvalStatus, String currentStatus, String stateExecutionInstanceId) {
    if (stateExecutionInstanceId == null) {
      return;
    }
    StateExecutionInstance stateExecutionInstance =
        stateExecutionService.getStateExecutionData(appId, stateExecutionInstanceId);
    if (stateExecutionInstance == null) {
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args", "No stateExecutionInstace found for id " + stateExecutionInstanceId);
    }

    ApprovalStateExecutionData executionData =
        (ApprovalStateExecutionData) stateExecutionInstance.getStateExecutionMap().get(
            stateExecutionInstance.getDisplayName());

    if (executionData.getCurrentStatus() != null && executionData.getCurrentStatus().equalsIgnoreCase(currentStatus)) {
      return;
    }
    executionData.setApprovedOn(System.currentTimeMillis());
    executionData.setCurrentStatus(currentStatus);

    logger.info("Saving executionData for approvalId: {}, workflowExecutionId: {} ", approvalId, workflowExecutionId);
    stateExecutionService.updateStateExecutionData(appId, stateExecutionInstanceId, executionData);
  }

  public JiraExecutionData getApprovalStatus(String connectorId, String accountId, String appId, String issueId,
      String approvalField, String approvalValue, String rejectionField, String rejectionValue) {
    JiraTaskParameters jiraTaskParameters = JiraTaskParameters.builder()
                                                .accountId(accountId)
                                                .issueId(issueId)
                                                .jiraAction(JiraAction.CHECK_APPROVAL)
                                                .approvalField(approvalField)
                                                .approvalValue(approvalValue)
                                                .rejectionField(rejectionField)
                                                .rejectionValue(rejectionValue)
                                                .build();
    JiraExecutionData jiraExecutionData = runTask(accountId, appId, connectorId, jiraTaskParameters);
    logger.info("Polling Approval for IssueId = {}", issueId);
    return jiraExecutionData;
  }

  public JiraExecutionData createJira(
      String accountId, String appId, String jiraConfigId, JiraTaskParameters jiraTaskParameters) {
    jiraTaskParameters.setJiraAction(JiraAction.CREATE_TICKET);
    return runTask(accountId, appId, jiraConfigId, jiraTaskParameters);
  }

  public JiraExecutionData getApprovalStatus(ApprovalPollingJobEntity approvalPollingJobEntity) {
    JiraTaskParameters jiraTaskParameters = JiraTaskParameters.builder()
                                                .accountId(approvalPollingJobEntity.getAccountId())
                                                .issueId(approvalPollingJobEntity.getIssueId())
                                                .jiraAction(JiraAction.CHECK_APPROVAL)
                                                .approvalField(approvalPollingJobEntity.getApprovalField())
                                                .approvalValue(approvalPollingJobEntity.getApprovalValue())
                                                .rejectionField(approvalPollingJobEntity.getRejectionField())
                                                .rejectionValue(approvalPollingJobEntity.getRejectionValue())
                                                .build();
    JiraExecutionData jiraExecutionData = runTask(approvalPollingJobEntity.getAccountId(),
        approvalPollingJobEntity.getAppId(), approvalPollingJobEntity.getConnectorId(), jiraTaskParameters);
    logger.info("Polling Approval for IssueId = {}", approvalPollingJobEntity.getIssueId());
    return jiraExecutionData;
  }
}
