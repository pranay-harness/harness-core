package software.wings.delegatetasks.jira;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Issue.FluentUpdate;
import net.rcarz.jiraclient.Issue.SearchResult;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.Resource;
import net.rcarz.jiraclient.RestException;
import net.rcarz.jiraclient.Transition;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.JiraExecutionData;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.JiraConfig;
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
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JiraTask extends AbstractDelegateRunnableTask {
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService logService;

  private static final String JIRA_APPROVAL_FIELD_KEY = "name";
  private static final String WEBHOOK_CREATION_URL = "/rest/webhooks/1.0/webhook/";

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

    ResponseData responseData = null;

    logger.info("Executing JiraTask. Action: {}, IssueId: {}", jiraAction, parameters.getIssueId());

    switch (jiraAction) {
      case AUTH:
        responseData = validateCredentials(parameters);
        break;

      case UPDATE_TICKET:
        responseData = updateTicket(parameters);
        break;

      case CREATE_TICKET:
        responseData = createTicket(parameters);
        break;

      case CREATE_WEBHOOK:
        responseData = createWebhook(parameters);
        break;

      case DELETE_WEBHOOK:
        responseData = deleteWebhook(parameters);
        break;

      case GET_PROJECTS:
        responseData = getProjects(parameters);
        break;

      case GET_FIELDS_OPTIONS:
        responseData = getFieldsAndOptions(parameters);
        break;

      case GET_STATUSES:
        responseData = getStatuses(parameters);
        break;

      case GET_CREATE_METADATA:
        responseData = getCreateMetadata(parameters);
        break;

      case CHECK_APPROVAL:
        responseData = checkJiraApproval(parameters);
        break;

      default:
        break;
    }

    if (responseData != null) {
      logger.info("Done executing JiraTask. Action: {}, IssueId: {}, ExecutionStatus: {}", jiraAction,
          parameters.getIssueId(), ((JiraExecutionData) responseData).getExecutionStatus());
    } else {
      logger.error("JiraTask Action: {}. IssueId: {}. null response.", jiraAction, parameters.getIssueId());
    }

    return responseData;
  }

  private ResponseData validateCredentials(JiraTaskParameters parameters) {
    try {
      JiraClient jiraClient = getJiraClient(parameters);
      jiraClient.getProjects();
    } catch (JiraException e) {
      String errorMessage = "Failed to fetch projects during credential validation.";
      logger.error(errorMessage, e);
      return JiraExecutionData.builder().errorMessage(errorMessage).executionStatus(ExecutionStatus.FAILED).build();
    }

    return JiraExecutionData.builder().executionStatus(ExecutionStatus.SUCCESS).build();
  }

  private ResponseData getCreateMetadata(JiraTaskParameters parameters) {
    URI uri = null;
    try {
      JiraClient jiraClient = getJiraClient(parameters);
      Map<String, String> queryParams = new HashMap<>();
      if (EmptyPredicate.isNotEmpty(parameters.getCreatemetaExpandParam())) {
        queryParams.put("expand", parameters.getCreatemetaExpandParam());
      } else {
        queryParams.put("expand", "projects.issuetypes.fields");
      }

      if (EmptyPredicate.isNotEmpty(parameters.getProject())) {
        queryParams.put("projectKeys", parameters.getProject());
      }

      uri = jiraClient.getRestClient().buildURI(Resource.getBaseUri() + "issue/createmeta", queryParams);

      JSON response = jiraClient.getRestClient().get(uri);

      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.SUCCESS)
          .createMetadata((JSONObject) response)
          .build();
    } catch (URISyntaxException | RestException | IOException | JiraException | RuntimeException e) {
      String errorMessage = "Failed to fetch issue metadata from Jira server.";
      logger.error(errorMessage, e);
      return JiraExecutionData.builder().errorMessage(errorMessage).executionStatus(ExecutionStatus.FAILED).build();
    }
  }

  private ResponseData getStatuses(JiraTaskParameters parameters) {
    try {
      JiraClient jiraClient = getJiraClient(parameters);
      URI uri = jiraClient.getRestClient().buildURI(
          Resource.getBaseUri() + "project/" + parameters.getProject() + "/statuses");
      JSON response = jiraClient.getRestClient().get(uri);

      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.SUCCESS)
          .statuses((JSONArray) response)
          .build();
    } catch (URISyntaxException | RestException | IOException | JiraException | RuntimeException e) {
      String errorMessage = "Failed to fetch statuses from Jira server.";
      logger.error(errorMessage, e);
      return JiraExecutionData.builder().errorMessage(errorMessage).executionStatus(ExecutionStatus.FAILED).build();
    }
  }

  private ResponseData getFieldsAndOptions(JiraTaskParameters parameters) {
    URI uri;
    SearchResult issues;
    JiraClient jiraClient;
    try {
      jiraClient = getJiraClient(parameters);
      String jqlQuery = "project = " + parameters.getProject();
      issues = jiraClient.searchIssues(jqlQuery, 1);
    } catch (JiraException | RuntimeException e) {
      String errorMessage = "Failed to fetch issues from Jira server for project - " + parameters.getProject();
      logger.error(errorMessage, e);
      return JiraExecutionData.builder().errorMessage(errorMessage).executionStatus(ExecutionStatus.FAILED).build();
    }

    Issue issue = null;
    if (CollectionUtils.isNotEmpty(issues.issues)) {
      issue = issues.issues.get(0);
    }
    String issueKey = (issue == null) ? (parameters.getProject() + "-1") : issue.getKey();

    try {
      uri = jiraClient.getRestClient().buildURI(Resource.getBaseUri() + "issue/" + issueKey + "/editmeta");
      JSON response = jiraClient.getRestClient().get(uri);

      return JiraExecutionData.builder().fields((JSONObject) response).executionStatus(ExecutionStatus.SUCCESS).build();
    } catch (URISyntaxException | IOException | RestException | RuntimeException e) {
      String errorMessage = "Failed to fetch editmeta from Jira server. Issue - " + issueKey;
      logger.error(errorMessage, e);
      return JiraExecutionData.builder().errorMessage(errorMessage).executionStatus(ExecutionStatus.FAILED).build();
    }
  }

  private ResponseData getProjects(JiraTaskParameters parameters) {
    try {
      JiraClient jira = getJiraClient(parameters);
      URI uri = jira.getRestClient().buildURI(Resource.getBaseUri() + "project");
      JSON response = jira.getRestClient().get(uri);
      JSONArray projectsArray = JSONArray.fromObject(response);
      return JiraExecutionData.builder().projects(projectsArray).executionStatus(ExecutionStatus.SUCCESS).build();
    } catch (URISyntaxException | IOException | RestException | JiraException | RuntimeException e) {
      String errorMessage = "Failed to fetch projects from Jira server.";
      logger.error(errorMessage, e);
      return JiraExecutionData.builder().errorMessage(errorMessage).executionStatus(ExecutionStatus.FAILED).build();
    }
  }

  private ResponseData updateTicket(JiraTaskParameters parameters) {
    CommandExecutionStatus commandExecutionStatus;
    Issue issue = null;
    try {
      JiraClient jira = getJiraClient(parameters);
      issue = jira.getIssue(parameters.getIssueId());
    } catch (JiraException e) {
      String errorMessage = "Failed to fetch jira issue : " + parameters.getIssueId();
      logger.error(errorMessage, e);
      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(errorMessage)
          .jiraServerResponse(extractResponseMessage(e))
          .build();
    }

    try {
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

      if (EmptyPredicate.isNotEmpty(parameters.getCustomFields())) {
        for (Entry<String, Object> customField : parameters.getCustomFields().entrySet()) {
          update.field(customField.getKey(), customField.getValue());
          fieldsUpdated = true;
        }
      }

      if (fieldsUpdated) {
        update.execute();
      }

      if (EmptyPredicate.isNotEmpty(parameters.getComment())) {
        issue.addComment(parameters.getComment());
      }

      if (EmptyPredicate.isNotEmpty(parameters.getStatus())) {
        updateStatus(issue, parameters.getStatus());
      }

      logger.info("Script execution finished with status: " + ExecutionStatus.SUCCESS);
      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.SUCCESS)
          .errorMessage("Updated Jira ticket " + issue.getKey())
          .issueUrl(getIssueUrl(parameters.getJiraConfig(), issue))
          .issueId(issue.getId())
          .issueKey(issue.getKey())
          .build();

    } catch (JiraException e) {
      String errorMessage = "Failed to update the new Jira ticket " + issue.getKey();
      logger.error(errorMessage, e);
      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(errorMessage)
          .jiraServerResponse(extractResponseMessage(e))
          .build();
    }
  }

  public void updateStatus(Issue issue, String status) throws JiraException {
    List<Transition> allTransitions = null;
    try {
      allTransitions = issue.getTransitions(); // gives all transitions available for that issue
    } catch (JiraException e) {
      logger.error("Failed to get all transitions from the Jira");
      throw e;
    }

    Transition transition =
        allTransitions.stream().filter(t -> t.getToStatus().getName().equalsIgnoreCase(status)).findAny().orElse(null);
    if (transition != null) {
      try {
        issue.transition().execute(transition);
      } catch (JiraException e) {
        logger.error("Exception while trying to update status to {}", status);
        throw e;
      }
    } else {
      logger.error("No transition found from {} to {}", issue.getStatus(), status);
      throw new JiraException("No transition found from [" + issue.getStatus() + "] to [" + status + "]");
    }
  }

  /**
   * Example error message :
   *
   * {400 : {"errorMessages":[],"errors":{"labels":"The label 'Test Application' contains spaces which is invalid."}}}
   *
   * @param e
   * @return
   */
  private String extractResponseMessage(Exception e) {
    if (e.getCause() != null) {
      String messageJson = "{" + e.getCause().getMessage() + "}";
      org.json.JSONObject jsonObject = null;
      try {
        jsonObject = new org.json.JSONObject(messageJson);
        Object[] keyArray = jsonObject.keySet().toArray();
        org.json.JSONObject innerJsonObject = jsonObject.getJSONObject((String) keyArray[0]);
        org.json.JSONArray jsonArray = (org.json.JSONArray) innerJsonObject.get("errorMessages");
        if (jsonArray.length() > 0) {
          return (String) jsonArray.get(0);
        }

        org.json.JSONObject errors = (org.json.JSONObject) innerJsonObject.get("errors");
        Object[] errorsKeys = errors.keySet().toArray();

        String errorsKey = (String) errorsKeys[0];
        return errorsKey + " : " + (String) errors.get((String) errorsKey);
      } catch (Exception ex) {
        logger.error("Failed to parse json response from Jira", ex);
      }
    }

    return e.getMessage();
  }

  private ResponseData createTicket(JiraTaskParameters parameters) {
    CommandExecutionStatus commandExecutionStatus;
    try {
      JiraClient jira = getJiraClient(parameters);
      Issue.FluentCreate fluentCreate = jira.createIssue(parameters.getProject(), parameters.getIssueType())
                                            .field(Field.SUMMARY, parameters.getSummary())
                                            .field(Field.DESCRIPTION, parameters.getDescription())
                                            .field(Field.LABELS, parameters.getLabels())
                                            .field(Field.PRIORITY, parameters.getPriority());

      if (EmptyPredicate.isNotEmpty(parameters.getCustomFields())) {
        for (Entry<String, Object> customField : parameters.getCustomFields().entrySet()) {
          fluentCreate.field(customField.getKey(), customField.getValue());
        }
      }

      Issue issue = fluentCreate.execute();

      if (isNotBlank(parameters.getStatus())) {
        issue.transition().execute(parameters.getStatus());
      }

      logger.info("Script execution finished with status SUCCESS");

      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.SUCCESS)
          .jiraAction(JiraAction.CREATE_TICKET)
          .errorMessage("Created Jira ticket " + issue.getKey())
          .issueId(issue.getId())
          .issueKey(issue.getKey())
          .issueUrl(getIssueUrl(parameters.getJiraConfig(), issue))
          .build();
    } catch (JiraException e) {
      logger.error("Unable to create a new Jira ticket", e);
      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage("Unable to create a new Jira ticket. ")
          .jiraServerResponse(extractResponseMessage(e))
          .build();
    }
  }

  private ResponseData checkJiraApproval(JiraTaskParameters parameters) {
    Issue issue;

    try {
      JiraClient jira = getJiraClient(parameters);
      issue = jira.getIssue(parameters.getIssueId());
    } catch (JiraException e) {
      CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.FAILURE;
      String errorMessage = "Failed to fetch jira issue for " + parameters.getIssueId();
      logger.error(errorMessage, e);
      return JiraExecutionData.builder().executionStatus(ExecutionStatus.FAILED).errorMessage(errorMessage).build();
    }

    logger.info("Issue fetched successfully");
    String approvalFieldValue = null;
    if (EmptyPredicate.isNotEmpty(parameters.getApprovalField())) {
      Map<String, String> fieldMap = (Map<String, String>) issue.getField(parameters.getApprovalField());
      approvalFieldValue = fieldMap.get(JIRA_APPROVAL_FIELD_KEY);
    }

    String rejectionFieldValue = null;
    if (EmptyPredicate.isNotEmpty(parameters.getRejectionField())) {
      Map<String, String> fieldMap = (Map<String, String>) issue.getField(parameters.getRejectionField());
      rejectionFieldValue = fieldMap.get(JIRA_APPROVAL_FIELD_KEY);
    }

    logger.info("IssueId: {}, approvalField: {}, approvalFieldValue: {}, rejectionField: {}, rejectionFieldValue: {}",
        parameters.getIssueId(), parameters.getApprovalField(), approvalFieldValue, parameters.getRejectionField(),
        rejectionFieldValue);

    if (EmptyPredicate.isNotEmpty(approvalFieldValue)
        && StringUtils.equalsIgnoreCase(approvalFieldValue, parameters.getApprovalValue())) {
      logger.info("IssueId: {} Approved", parameters.getIssueId());
      return JiraExecutionData.builder().executionStatus(ExecutionStatus.SUCCESS).build();
    }

    if (EmptyPredicate.isNotEmpty(rejectionFieldValue)
        && StringUtils.equalsIgnoreCase(rejectionFieldValue, parameters.getRejectionValue())) {
      logger.info("IssueId: {} Rejected", parameters.getIssueId());
      return JiraExecutionData.builder().executionStatus(ExecutionStatus.REJECTED).build();
    }

    return JiraExecutionData.builder().executionStatus(ExecutionStatus.RUNNING).build();
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

  private JiraClient getJiraClient(JiraTaskParameters parameters) throws JiraException {
    JiraConfig jiraConfig = parameters.getJiraConfig();
    encryptionService.decrypt(jiraConfig, parameters.getEncryptionDetails());
    BasicCredentials creds = new BasicCredentials(jiraConfig.getUsername(), new String(jiraConfig.getPassword()));

    return new JiraClient(jiraConfig.getBaseUrl(), creds);
  }

  private ResponseData deleteWebhook(JiraTaskParameters parameters) {
    JiraConfig jiraConfig = parameters.getJiraConfig();
    encryptionService.decrypt(jiraConfig, parameters.getEncryptionDetails());

    Issue issue;
    CommandExecutionStatus commandExecutionStatus;
    JiraClient jira;
    try {
      jira = getJiraClient(parameters);
      issue = jira.getIssue(parameters.getIssueId());
    } catch (JiraException e) {
      String error = "Unable to fetch Jira for id: " + parameters.getIssueId();
      logger.error(error, e);
      return JiraExecutionData.builder().executionStatus(ExecutionStatus.FAILED).errorMessage(error).build();
    }
    String message = "Approval/Rejection provided on ticket: " + issue.getKey();
    JiraExecutionData jiraExecutionData = JiraExecutionData.builder()
                                              .issueUrl(getIssueUrl(jiraConfig, issue))
                                              .executionStatus(ExecutionStatus.SUCCESS)
                                              .errorMessage(message)
                                              .build();
    if (parameters.getWebhookUrl() == null) {
      return jiraExecutionData;
    }
    try {
      jira.getRestClient().delete(new URI(parameters.getWebhookUrl()));
    } catch (IOException | URISyntaxException | RestException e) {
      logger.error("Unable to delete a new Jira webhook", e);
      return jiraExecutionData;
    }
    return jiraExecutionData;
  }

  private ResponseData createWebhook(JiraTaskParameters parameters) {
    JiraConfig jiraConfig = parameters.getJiraConfig();
    encryptionService.decrypt(jiraConfig, parameters.getEncryptionDetails());
    JiraClient jira;
    CommandExecutionStatus commandExecutionStatus;
    Issue issue;
    try {
      jira = getJiraClient(parameters);
      issue = jira.getIssue(parameters.getIssueId());
    } catch (JiraException e) {
      String error = "Unable to fetch Jira for id: " + parameters.getIssueId();
      logger.error(error, e);
      return JiraExecutionData.builder().executionStatus(ExecutionStatus.FAILED).errorMessage(error).build();
    }

    List<String> events = new ArrayList<>();
    events.add("jira:issue_updated");
    Map<String, String> filters = new HashMap<>();
    filters.put("issue-related-events-section", "issue = " + parameters.getIssueId());
    String url = parameters.getCallbackUrl();
    String message = "Waiting for Approval on ticket: " + issue.getKey();
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
    JiraExecutionData jiraExecutionData = JiraExecutionData.builder()
                                              .executionStatus(ExecutionStatus.PAUSED)
                                              .issueUrl(getIssueUrl(jiraConfig, issue))
                                              .errorMessage(message)
                                              .build();
    try {
      JSON resp = jira.getRestClient().post(new URI(jiraConfig.getBaseUrl() + WEBHOOK_CREATION_URL), json);
      JSONObject object = JSONObject.fromObject(resp);
      webhookUrl = object.getString("self");
    } catch (RestException | IOException | URISyntaxException e) {
      String error = "Unable to create a new Jira webhook for " + getIssueUrl(jiraConfig, issue);
      logger.error(error, e);
      return jiraExecutionData;
    }
    jiraExecutionData.setWebhookUrl(webhookUrl);
    return jiraExecutionData;
  }
}
