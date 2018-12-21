package software.wings.delegatetasks.jira;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.delegate.task.protocol.TaskParameters;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Issue.FluentUpdate;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.Resource;
import net.rcarz.jiraclient.RestException;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.JiraExecutionData;
import software.wings.app.MainConfiguration;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.JiraConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.jira.JiraTaskParameters;
import software.wings.beans.jira.JiraWebhookParameters;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JiraTask extends AbstractDelegateRunnableTask {
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService logService;
  @Inject private MainConfiguration mainConfiguration;

  private static final String WEBHOOK_CREATION_URL = "/rest/webhooks/1.0/webhook/";
  private static final String JIRA_APPROVAL_API_PATH = "api/ticketing/jira-approval/";

  private static final Logger logger = LoggerFactory.getLogger(JiraTask.class);

  public JiraTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public ResponseData run(Object[] parameters) {
    return run((JiraTaskParameters) parameters[0]);
  }

  public ResponseData run(JiraTaskParameters parameters) {
    JiraAction jiraAction = parameters.getJiraAction();

    switch (jiraAction) {
      case AUTH:
        break;

      case UPDATE_TICKET:
        return updateTicket(parameters);

      case CREATE_TICKET:
        return createTicket(parameters);

      case CREATE_WEBHOOK:
        return createWebhook(parameters);

      case DELETE_WEBHOOK:
        return deleteWebhook(parameters);

      case GET_PROJECTS:
        return getProjects(parameters);

      case GET_FIELDS_OPTIONS:
        return getFieldsAndOptions(parameters);

      case GET_STATUSES:
        return getStatuses(parameters);

      case GET_CREATE_METADATA:
        return getCreateMetadata(parameters);

      case CHECK_APPROVAL:
        return checkJiraApproval(parameters);

      default:
        break;
    }

    return null;
  }

  private ResponseData getCreateMetadata(JiraTaskParameters parameters) {
    JiraClient jiraClient = getJiraClient(parameters);

    URI uri = null;
    try {
      Map<String, String> queryParams = new HashMap<>();
      queryParams.put("expand", "projects.issuetypes.fields");

      uri = jiraClient.getRestClient().buildURI(Resource.getBaseUri() + "issue/createmeta", queryParams);

      JSON response = jiraClient.getRestClient().get(uri);

      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.SUCCESS)
          .createMetadata((JSONObject) response)
          .build();
    } catch (URISyntaxException | RestException | IOException e) {
      String errorMessage = "Failed to fetch statuses from JIRA server.";
      logger.error(errorMessage);
      return JiraExecutionData.builder().errorMessage(errorMessage).build();
    }
  }

  private ResponseData getStatuses(JiraTaskParameters parameters) {
    JiraClient jiraClient = getJiraClient(parameters);

    URI uri = null;
    try {
      uri = jiraClient.getRestClient().buildURI(
          Resource.getBaseUri() + "project/" + parameters.getProject() + "/statuses");
      JSON response = jiraClient.getRestClient().get(uri);

      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.SUCCESS)
          .statuses((JSONArray) response)
          .build();
    } catch (URISyntaxException | RestException | IOException e) {
      String errorMessage = "Failed to fetch statuses from JIRA server.";
      logger.error(errorMessage);
      return JiraExecutionData.builder().errorMessage(errorMessage).build();
    }
  }

  private ResponseData getFieldsAndOptions(JiraTaskParameters parameters) {
    JiraClient jiraClient = getJiraClient(parameters);

    URI uri;
    try {
      String issueKey = parameters.getProject() + "-1";
      uri = jiraClient.getRestClient().buildURI(Resource.getBaseUri() + "issue/" + issueKey + "/editmeta");
      JSON response = jiraClient.getRestClient().get(uri);

      return JiraExecutionData.builder().fields((JSONObject) response).build();
    } catch (URISyntaxException | IOException | RestException e) {
      String errorMessage = "Failed to fetch fields from JIRA server.";
      logger.error(errorMessage);

      // TODO(swagat): Add execution status to JiraExecutionData
      return JiraExecutionData.builder().errorMessage(errorMessage).build();
    }
  }

  private ResponseData getProjects(JiraTaskParameters parameters) {
    JiraClient jira = getJiraClient(parameters);

    JSONArray projectsArray = null;
    try {
      URI uri = jira.getRestClient().buildURI(Resource.getBaseUri() + "project");
      JSON response = jira.getRestClient().get(uri);
      projectsArray = JSONArray.fromObject(response);
    } catch (URISyntaxException | IOException | RestException e) {
      String errorMessage = "Failed to fetch projects from JIRA server.";
      logger.error(errorMessage);

      return JiraExecutionData.builder().errorMessage(errorMessage).build();
    }

    return JiraExecutionData.builder().projects(projectsArray).build();
  }

  private ResponseData updateTicket(JiraTaskParameters parameters) {
    JiraClient jira = getJiraClient(parameters);

    Issue issue = null;
    CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.RUNNING;
    try {
      issue = jira.getIssue(parameters.getIssueId());
      boolean fieldsUpdated = false;

      FluentUpdate update = issue.update();
      if (EmptyPredicate.isNotEmpty(parameters.getSummary())) {
        update.field(Field.SUMMARY, parameters.getSummary());
        fieldsUpdated = true;
      }

      if (EmptyPredicate.isNotEmpty(parameters.getLabels())) {
        update.field(Field.LABELS, parameters.getLabels());
        fieldsUpdated = true;
      }

      if (fieldsUpdated) {
        update.execute();
      }

      if (EmptyPredicate.isNotEmpty(parameters.getComment())) {
        issue.addComment(parameters.getComment());
      }

      if (EmptyPredicate.isNotEmpty(parameters.getStatus())) {
        issue.transition().execute(parameters.getStatus());
      }

    } catch (JiraException e) {
      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

      String errorMessage = "Failed to update the new JIRA ticket " + parameters.getIssueId();
      logger.error(errorMessage, e);
      return JiraExecutionData.builder().executionStatus(ExecutionStatus.FAILED).errorMessage(errorMessage).build();
    }

    commandExecutionStatus = CommandExecutionStatus.SUCCESS;
    saveExecutionLog(
        parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

    return JiraExecutionData.builder()
        .executionStatus(ExecutionStatus.SUCCESS)
        .errorMessage("Updated JIRA ticket " + issue.getKey())
        .issueUrl(getIssueUrl(parameters.getJiraConfig(), issue))
        .build();
  }

  private ResponseData createTicket(JiraTaskParameters parameters) {
    JiraClient jira = getJiraClient(parameters);
    Issue issue;

    CommandExecutionStatus commandExecutionStatus;
    try {
      issue = jira.createIssue(parameters.getProject(), parameters.getIssueType())
                  .field(Field.SUMMARY, parameters.getSummary())
                  .field(Field.DESCRIPTION, parameters.getDescription())
                  .field(Field.ASSIGNEE, parameters.getAssignee())
                  .field(Field.LABELS, parameters.getLabels())
                  .execute();

      if (isNotBlank(parameters.getStatus())) {
        issue.transition().execute(parameters.getStatus());
      }
      commandExecutionStatus = CommandExecutionStatus.SUCCESS;
    } catch (JiraException e) {
      logger.error("Unable to create a new JIRA ticket", e);
      commandExecutionStatus = CommandExecutionStatus.FAILURE;

      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);
      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage("Unable to create the new JIRA ticket " + parameters.getIssueId())
          .build();
    }

    saveExecutionLog(
        parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

    return JiraExecutionData.builder()
        .executionStatus(ExecutionStatus.SUCCESS)
        .jiraAction(JiraAction.CREATE_TICKET)
        .errorMessage("Created JIRA ticket " + issue.getKey())
        .issueId(issue.getId())
        .issueUrl(getIssueUrl(parameters.getJiraConfig(), issue))
        .build();
  }

  private ResponseData checkJiraApproval(JiraTaskParameters parameters) {
    JiraClient jira = getJiraClient(parameters);
    Issue issue;
    CommandExecutionStatus commandExecutionStatus;
    try {
      issue = jira.getIssue(parameters.getIssueId());
    } catch (JiraException e) {
      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

      String errorMessage = "Failed to fetch jira issue for " + parameters.getIssueId();
      logger.error(errorMessage, e);
      return JiraExecutionData.builder().executionStatus(ExecutionStatus.FAILED).errorMessage(errorMessage).build();
    }
    String approvalFieldValue = issue.getField(parameters.getApprovalField()).toString();
    String rejectionFieldValue = issue.getField(parameters.getApprovalField()).toString();

    if (StringUtils.equals(approvalFieldValue, parameters.getApprovalValue())) {
      return JiraExecutionData.builder().executionStatus(ExecutionStatus.SUCCESS).build();
    } else if (StringUtils.equals(rejectionFieldValue, parameters.getRejectionValue())) {
      return JiraExecutionData.builder().executionStatus(ExecutionStatus.REJECTED).build();
    }
    return JiraExecutionData.builder().executionStatus(ExecutionStatus.SUCCESS).build();
  }

  private String getIssueUrl(JiraConfig jiraConfig, Issue issue) {
    try {
      URL jiraUrl = new URL(jiraConfig.getBaseUrl());
      URL issueUrl = new URL(jiraUrl, "/browse/" + issue.getKey());

      return issueUrl.toString();
    } catch (MalformedURLException e) {
      logger.info("Incorrect url");
    }

    return null;
  }

  private void saveExecutionLog(
      JiraTaskParameters parameters, String line, CommandExecutionStatus commandExecutionStatus) {
    logService.save(parameters.getAccountId(),
        aLog()
            .withAppId(parameters.getAppId())
            .withActivityId(parameters.getActivityId())
            .withLogLevel(INFO)
            .withLogLine(line)
            .withExecutionResult(commandExecutionStatus)
            .build());
  }

  private JiraClient getJiraClient(JiraTaskParameters parameters) {
    JiraConfig jiraConfig = parameters.getJiraConfig();
    encryptionService.decrypt(jiraConfig, parameters.getEncryptionDetails());
    BasicCredentials creds = new BasicCredentials(jiraConfig.getUsername(), new String(jiraConfig.getPassword()));

    return new JiraClient(jiraConfig.getBaseUrl(), creds);
  }

  private ResponseData deleteWebhook(JiraTaskParameters parameters) {
    JiraConfig jiraConfig = parameters.getJiraConfig();
    encryptionService.decrypt(jiraConfig, parameters.getEncryptionDetails());
    JiraClient jira = getJiraClient(parameters);
    Issue issue;
    CommandExecutionStatus commandExecutionStatus;
    try {
      issue = jira.getIssue(parameters.getIssueId());
    } catch (JiraException e) {
      String error = "Not deleting webhook as unable to fetch Jira for id: " + parameters.getIssueId();
      logger.error(error, e);
      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

      return JiraExecutionData.builder().executionStatus(ExecutionStatus.FAILED).errorMessage(error).build();
    }

    try {
      jira.getRestClient().delete(new URI(parameters.getWebhookUrl()));
    } catch (IOException | URISyntaxException | RestException e) {
      logger.error("Unable to delete a new JIRA webhook", e);
      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage("Unable to delete the JIRA webhook " + parameters.getIssueId())
          .build();
    }
    commandExecutionStatus = CommandExecutionStatus.SUCCESS;
    saveExecutionLog(
        parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

    return JiraExecutionData.builder()
        .executionStatus(ExecutionStatus.SUCCESS)
        .errorMessage("Approval provided on ticket: " + issue.getKey())
        .issueUrl(getIssueUrl(jiraConfig, issue))
        .build();
  }

  private ResponseData createWebhook(JiraTaskParameters parameters) {
    JiraConfig jiraConfig = parameters.getJiraConfig();
    encryptionService.decrypt(jiraConfig, parameters.getEncryptionDetails());
    JiraClient jira = getJiraClient(parameters);
    CommandExecutionStatus commandExecutionStatus;
    Issue issue;
    try {
      issue = jira.getIssue(parameters.getIssueId());
    } catch (JiraException e) {
      String error = "Not creating webhook as unable to fetch Jira for id: " + parameters.getIssueId();
      logger.error(error, e);
      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

      return JiraExecutionData.builder().executionStatus(ExecutionStatus.FAILED).errorMessage(error).build();
    }

    List<String> events = new ArrayList<>();
    events.add("jira:issue_updated");

    Map<String, String> filters = new HashMap<>();
    filters.put("issue-related-events-section", "issue = " + parameters.getIssueId());
    String token = parameters.getJiraToken();

    // Todo: Replace hardcoded url after checking the Url from config

    String url = getBaseUrl() + JIRA_APPROVAL_API_PATH + token;

    JiraWebhookParameters jiraWebhookParameters = JiraWebhookParameters.builder()
                                                      .name("webhook for issue = " + issue.getKey())
                                                      .events(events)
                                                      .filters(filters)
                                                      .excludeBody(false)
                                                      .jqlFilter(filters)
                                                      .excludeIssueDetails(false)
                                                      .url(url)
                                                      .build();

    JSONObject json = JSONObject.fromObject(jiraWebhookParameters);

    String webhookUrl;
    try {
      JSON resp = jira.getRestClient().post(new URI(jiraConfig.getBaseUrl() + WEBHOOK_CREATION_URL), json);
      JSONObject object = JSONObject.fromObject(resp);
      webhookUrl = object.getString("self");
    } catch (RestException | IOException | URISyntaxException e) {
      String error = "Unable to create a new JIRA webhook for " + getIssueUrl(jiraConfig, issue);
      logger.error(error, e);

      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

      return JiraExecutionData.builder().executionStatus(ExecutionStatus.FAILED).errorMessage(error).build();
    }

    commandExecutionStatus = CommandExecutionStatus.SUCCESS;
    saveExecutionLog(
        parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

    return JiraExecutionData.builder()
        .executionStatus(ExecutionStatus.SUCCESS)
        .webhookUrl(webhookUrl)
        .errorMessage("Waiting for Approval on ticket: " + issue.getKey())
        .issueUrl(getIssueUrl(jiraConfig, issue))
        .build();
  }

  private String getBaseUrl() {
    String baseUrl = mainConfiguration.getPortal().getUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return baseUrl;
  }
}
