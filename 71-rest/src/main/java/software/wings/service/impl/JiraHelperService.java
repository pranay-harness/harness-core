package software.wings.service.impl;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Inject;

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
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.security.SecretManager;

import java.util.concurrent.TimeUnit;

/**
 * All Jira apis should be accessed via this object.
 */
public class JiraHelperService {
  private static final Logger logger = LoggerFactory.getLogger(GcpHelperService.class);
  private static final String WORKFLOW_EXECUTION_ID = "workflow";
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DelegateServiceImpl delegateService;
  @Inject @Transient private transient SecretManager secretManager;

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

  public JSONArray getProjects(JiraConfig jiraConfig, String accountId, String appId) {
    DelegateTask delegateTask =
        aDelegateTask()
            .withTaskType(TaskType.JIRA)
            .withAccountId(accountId)
            .withAppId(appId)
            .withParameters(new Object[] {
                JiraTaskParameters.builder()
                    .accountId(accountId)
                    .jiraAction(JiraAction.GET_PROJECTS)
                    .jiraConfig(jiraConfig)
                    .encryptionDetails(secretManager.getEncryptionDetails(jiraConfig, appId, WORKFLOW_EXECUTION_ID))
                    .build()})
            .withTimeout(TimeUnit.SECONDS.toMillis(30))
            .withAsync(false)
            .build();

    JiraExecutionData jiraExecutionData = delegateService.executeTask(delegateTask);
    return jiraExecutionData.getProjects();
  }

  public Object getFields(JiraConfig jiraConfig, String accountId, String appId) {
    DelegateTask delegateTask =
        aDelegateTask()
            .withTaskType(TaskType.JIRA)
            .withAccountId(accountId)
            .withAppId(appId)
            .withParameters(new Object[] {
                JiraTaskParameters.builder()
                    .accountId(accountId)
                    .jiraAction(JiraAction.GET_FIELDS)
                    .jiraConfig(jiraConfig)
                    .encryptionDetails(secretManager.getEncryptionDetails(jiraConfig, appId, WORKFLOW_EXECUTION_ID))
                    .build()})
            .withTimeout(TimeUnit.SECONDS.toMillis(30))
            .withAsync(false)
            .build();

    JiraExecutionData jiraExecutionData = delegateService.executeTask(delegateTask);
    return jiraExecutionData.getFields();
  }
}
