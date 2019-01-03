package software.wings.service.impl;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.delegatetasks.jira.JiraAction.CREATE_WEBHOOK;
import static software.wings.delegatetasks.jira.JiraAction.DELETE_WEBHOOK;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.interfaces.Claim;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.waiter.WaitNotifyEngine;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.JiraExecutionData;
import software.wings.app.MainConfiguration;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.DelegateTask;
import software.wings.beans.JiraConfig;
import software.wings.beans.TaskType;
import software.wings.beans.approval.JiraApprovalParams;
import software.wings.beans.jira.JiraTaskParameters;
import software.wings.delegatetasks.jira.JiraAction;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * All Jira apis should be accessed via this object.
 */
@Singleton
public class JiraHelperService {
  private static final Logger logger = LoggerFactory.getLogger(GcpHelperService.class);
  private static final String WORKFLOW_EXECUTION_ID = "workflow";
  private static final long JIRA_DELEGATE_TIMEOUT_MILLIS = 30 * 1000;
  @Inject private DelegateServiceImpl delegateService;
  @Inject @Transient private transient SecretManager secretManager;
  @Inject SettingsService settingService;
  @Inject WorkflowExecutionService workflowExecutionService;
  @Inject WaitNotifyEngine waitNotifyEngine;
  @Inject private MainConfiguration mainConfiguration;

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
   *
   */
  public void validateCredential(JiraConfig jiraConfig) {
    BasicCredentials creds = new BasicCredentials(jiraConfig.getUsername(), new String(jiraConfig.getPassword()));
    JiraClient jira = new JiraClient(jiraConfig.getBaseUrl(), creds);
    try {
      jira.getProjects();
    } catch (JiraException e) {
      logger.error("[JIRA] Invalid url or credentials");
      logger.info(e.getMessage());
      throw new InvalidRequestException(
          "Failed to Authenticate with JIRA Server. " + extractRelevantMessage(e.getMessage()));
    }
  }

  private String extractRelevantMessage(String message) {
    String[] words = message.split("\\s+");

    return words[0] + " " + words[1];
  }

  public JSONArray getProjects(String connectorId, String accountId, String appId) {
    JiraTaskParameters jiraTaskParameters =
        JiraTaskParameters.builder().accountId(accountId).jiraAction(JiraAction.GET_PROJECTS).build();

    JiraExecutionData jiraExecutionData = runTask(accountId, appId, connectorId, jiraTaskParameters);
    return jiraExecutionData.getProjects();
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
    return jiraExecutionData.getFields();
  }

  public String createJiraToken(String appId, String workflowExecutionId, String approvalId, String approvalField,
      String approvalValue, String rejectionFiled, String rejectionValue) {
    Map<String, String> claimsMap = new HashMap<>();
    claimsMap.put(APP_ID_KEY, appId);
    claimsMap.put(WORKFLOW_EXECUTION_ID_KEY, workflowExecutionId);
    claimsMap.put(APPROVAL_FIELD_KEY, approvalField);
    claimsMap.put(APPROVAL_VALUE_KEY, approvalValue);
    claimsMap.put(APPROVAL_ID_KEY, approvalId);
    claimsMap.put(REJECTION_FIELD_KEY, rejectionFiled);
    claimsMap.put(REJECTION_VALUE_KEY, rejectionValue);

    return secretManagerForToken.generateJWTToken(ImmutableMap.copyOf(claimsMap), JWT_CATEGORY.JIRA_SERVICE_SECRET);
  }

  public Map<String, Claim> validateJiraToken(String token) {
    return secretManagerForToken.verifyJWTToken(token, JWT_CATEGORY.JIRA_SERVICE_SECRET);
  }

  public Object getStatuses(String connectorId, String project, String accountId, String appId) {
    JiraTaskParameters jiraTaskParameters =
        JiraTaskParameters.builder().accountId(accountId).jiraAction(JiraAction.GET_STATUSES).project(project).build();

    JiraExecutionData jiraExecutionData = runTask(accountId, appId, connectorId, jiraTaskParameters);
    return jiraExecutionData.getStatuses();
  }

  public JiraExecutionData createWebhook(JiraApprovalParams jiraApprovalParams, String accountId, String appId,
      String workflowExecutionId, String approvalId) {
    String token = createJiraToken(appId, workflowExecutionId, approvalId, jiraApprovalParams.getApprovalField(),
        jiraApprovalParams.getApprovalValue(), jiraApprovalParams.getRejectionField(),
        jiraApprovalParams.getRejectionValue());
    String url = getBaseUrl() + JIRA_APPROVAL_API_PATH + token;
    JiraTaskParameters jiraTaskParameters = JiraTaskParameters.builder()
                                                .accountId(accountId)
                                                .appId(appId)
                                                .approvalId(approvalId)
                                                .jiraAction(CREATE_WEBHOOK)
                                                .issueId(jiraApprovalParams.getIssueId())
                                                .callbackUrl(url)
                                                .build();
    return runTask(accountId, appId, jiraApprovalParams.getJiraConnectorId(), jiraTaskParameters);
  }

  private String getBaseUrl() {
    String baseUrl = mainConfiguration.getPortal().getUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return baseUrl;
  }

  public JiraExecutionData deleteWebhook(
      JiraApprovalParams jiraApprovalParams, String webhookUrl, String appId, String accountId) {
    JiraTaskParameters parameters = JiraTaskParameters.builder()
                                        .jiraAction(DELETE_WEBHOOK)
                                        .webhookUrl(webhookUrl)
                                        .issueId(jiraApprovalParams.getIssueId())
                                        .appId(appId)
                                        .accountId(accountId)
                                        .build();
    return runTask(accountId, appId, jiraApprovalParams.getJiraConnectorId(), parameters);
  }

  private JiraExecutionData runTask(
      String accountId, String appId, String connectorId, JiraTaskParameters jiraTaskParameters) {
    JiraConfig jiraConfig = (JiraConfig) settingService.get(connectorId).getValue();
    jiraTaskParameters.setJiraConfig(jiraConfig);
    jiraTaskParameters.setEncryptionDetails(
        secretManager.getEncryptionDetails(jiraConfig, appId, WORKFLOW_EXECUTION_ID));

    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.JIRA)
                                    .withAccountId(accountId)
                                    .withAppId(appId)
                                    .withParameters(new Object[] {jiraTaskParameters})
                                    .withTimeout(JIRA_DELEGATE_TIMEOUT_MILLIS)
                                    .withAsync(false)
                                    .build();

    ResponseData responseData = delegateService.executeTask(delegateTask);

    if (responseData instanceof JiraExecutionData) {
      return (JiraExecutionData) responseData;
    } else {
      return JiraExecutionData.builder().errorMessage("Delegate task failed with an exception").build();
    }
  }

  public Object getCreateMetadata(String connectorId, String accountId, String appId) {
    JiraTaskParameters jiraTaskParameters =
        JiraTaskParameters.builder().accountId(accountId).jiraAction(JiraAction.GET_CREATE_METADATA).build();

    JiraExecutionData jiraExecutionData = runTask(accountId, appId, connectorId, jiraTaskParameters);
    return jiraExecutionData.getCreateMetadata();
  }

  public ExecutionStatus checkApprovalFromWebhookCallback(String token, String respJson) {
    Map<String, Claim> claimMap = this.validateJiraToken(token);
    String appId = claimMap.get(APP_ID_KEY).asString();
    String workflowExecutionId = claimMap.get(WORKFLOW_EXECUTION_ID_KEY).asString();
    String approvalField = claimMap.get(APPROVAL_FIELD_KEY).asString().toLowerCase();
    String approvalValue = claimMap.get(APPROVAL_VALUE_KEY).asString().toLowerCase();
    String rejectionField = claimMap.get(REJECTION_FIELD_KEY).asString().toLowerCase();
    String rejectionValue = claimMap.get(REJECTION_VALUE_KEY).asString().toLowerCase();
    String approvalId = claimMap.get(APPROVAL_ID_KEY).asString();

    JSONObject jsonObject = JSONObject.fromObject(respJson);
    JSONObject changeLog = jsonObject.getJSONObject("changelog");
    JSONArray items = changeLog.getJSONArray("items");

    ExecutionStatus approvalStatus = null;
    Action action = null;
    for (int i = 0; i < items.size(); i++) {
      JSONObject item = items.getJSONObject(i);
      if (Objects.equals(item.getString("field").toLowerCase(), approvalField)
          && Objects.equals(item.getString("toString").toLowerCase(), approvalValue)) {
        approvalStatus = ExecutionStatus.SUCCESS;
        action = Action.APPROVE;
      }
      if (Objects.equals(item.getString("field").toLowerCase(), rejectionField)
          && Objects.equals(item.getString("toString").toLowerCase(), rejectionValue)) {
        approvalStatus = ExecutionStatus.REJECTED;
        action = Action.REJECT;
      }
    }

    if (approvalStatus != null) {
      JSONObject jiraUser = jsonObject.getJSONObject("user");
      String username = jiraUser.getString("name");
      String email = jiraUser.getString("emailAddress");

      EmbeddedUser user = new EmbeddedUser(null, username, email);

      ApprovalDetails approvalDetails = new ApprovalDetails();
      approvalDetails.setAction(action);
      approvalDetails.setApprovalId(approvalId);
      approvalDetails.setApprovedBy(user);
      ApprovalStateExecutionData executionData =
          workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
              appId, workflowExecutionId, null, approvalDetails);
      executionData.setStatus(approvalStatus);
      executionData.setApprovedOn(System.currentTimeMillis());
      executionData.setApprovedBy(user);
      waitNotifyEngine.notify(approvalId, executionData);
    }
    return approvalStatus;
  }
}
