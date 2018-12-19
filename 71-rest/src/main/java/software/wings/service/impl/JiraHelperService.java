package software.wings.service.impl;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.interfaces.Claim;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.sf.json.JSONArray;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.JiraExecutionData;
import software.wings.beans.DelegateTask;
import software.wings.beans.JiraConfig;
import software.wings.beans.TaskType;
import software.wings.beans.jira.JiraTaskParameters;
import software.wings.delegatetasks.jira.JiraAction;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.Map;

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

  public static final String APP_ID_KEY = "app_id";
  public static final String WORKFLOW_EXECUTION_ID_KEY = "workflow_execution_id";
  public static final String APPROVAL_FIELD_KEY = "approval_field";
  public static final String APPROVAL_VALUE_KEY = "approval_value";
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

  public String createJiraToken(
      String appId, String workflowExecutionId, String approvalId, String approvalField, String approvalValue) {
    return secretManagerForToken.generateJWTToken(
        ImmutableMap.of(APP_ID_KEY, appId, WORKFLOW_EXECUTION_ID_KEY, workflowExecutionId, APPROVAL_FIELD_KEY,
            approvalField, APPROVAL_VALUE_KEY, approvalValue, APPROVAL_ID_KEY, approvalId),
        JWT_CATEGORY.JIRA_SERVICE_SECRET);
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
}
