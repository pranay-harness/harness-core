package io.harness.delegate.task.jira;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse.JiraIssueData;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.jira.JiraAction;
import io.harness.jira.JiraCreateMetaResponse;
import io.harness.jira.JiraCustomFieldValue;
import io.harness.jira.JiraField;
import io.harness.jira.JiraIssueType;
import io.harness.jira.JiraProjectData;
import lombok.extern.slf4j.Slf4j;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Field.ValueTuple;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Issue.FluentCreate;
import net.rcarz.jiraclient.Issue.FluentUpdate;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.Resource;
import net.rcarz.jiraclient.RestException;
import net.rcarz.jiraclient.TimeTracking;
import net.rcarz.jiraclient.Transition;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.rcarz.jiraclient.Project;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static net.rcarz.jiraclient.Field.RESOLUTION;
import static net.rcarz.jiraclient.Field.TIME_TRACKING;

@Singleton
@Slf4j
public class JiraTaskNGHandler {
  @VisibleForTesting static final String ORIGINAL_ESTIMATE = "TimeTracking:OriginalEstimate";
  @VisibleForTesting static final String REMAINING_ESTIMATE = "TimeTracking:RemainingEstimate";
  @VisibleForTesting static final String JIRA_APPROVAL_FIELD_KEY = "name";

  public JiraTaskNGResponse validateCredentials(JiraTaskNGParameters jiraTaskNGParameters) {
    try {
      JiraClient jiraClient = getJiraClient(jiraTaskNGParameters);
      jiraClient.getProjects();
    } catch (JiraException e) {
      String errorMessage = "Failed to fetch projects during credential validation.";
      log.error(errorMessage, e);
      return JiraTaskNGResponse.builder().errorMessage(errorMessage).executionStatus(FAILURE).build();
    }

    return JiraTaskNGResponse.builder().executionStatus(SUCCESS).build();
  }

  public JiraTaskNGResponse createTicket(JiraTaskNGParameters jiraTaskNGParameters) {
    try {
      JiraClient jiraClient = getJiraClient(jiraTaskNGParameters);
      FluentCreate fluentCreate =
          jiraClient.createIssue(jiraTaskNGParameters.getProject(), jiraTaskNGParameters.getIssueType())
              .field(Field.SUMMARY, jiraTaskNGParameters.getSummary())
              .field(Field.DESCRIPTION, jiraTaskNGParameters.getDescription());

      if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getPriority())) {
        fluentCreate.field(Field.PRIORITY, jiraTaskNGParameters.getPriority());
      }

      if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getLabels())) {
        fluentCreate.field(Field.LABELS, jiraTaskNGParameters.getLabels());
      }

      if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getCustomFields())) {
        setCustomFieldsOnCreate(jiraTaskNGParameters, fluentCreate);
      }

      Issue issue = fluentCreate.execute();

      log.info("Script execution finished with status SUCCESS");

      return JiraTaskNGResponse.builder()
          .executionStatus(SUCCESS)
          .jiraAction(JiraAction.CREATE_TICKET)
          .issueId(issue.getId())
          .issueKey(issue.getKey())
          .issueUrl(getIssueUrl(jiraTaskNGParameters.getJiraConnectorDTO(), issue.getKey()))
          .jiraIssueData(JiraIssueData.builder().description(issue.getDescription()).build())
          .build();
    } catch (JiraException e) {
      String errorMessage = "Failed to create Jira ticket";
      log.error(errorMessage, e);
      return JiraTaskNGResponse.builder()
          .errorMessage(
              "Unable to create a new Jira ticket. " + ExceptionUtils.getMessage(e) + " " + extractResponseMessage(e))
          .executionStatus(FAILURE)
          .build();
    } catch (WingsException e) {
      return JiraTaskNGResponse.builder().executionStatus(FAILURE).errorMessage(ExceptionUtils.getMessage(e)).build();
    }
  }

  public JiraTaskNGResponse updateTicket(JiraTaskNGParameters jiraTaskNGParameters) {
    JiraClient jiraClient;
    try {
      jiraClient = getJiraClient(jiraTaskNGParameters);
    } catch (JiraException j) {
      String errorMessage =
          "Failed to create jira client while trying to update : " + jiraTaskNGParameters.getUpdateIssueIds();
      return JiraTaskNGResponse.builder().errorMessage(errorMessage).executionStatus(FAILURE).build();
    }

    List<String> issueKeys = new ArrayList<>();
    List<String> issueUrls = new ArrayList<>();
    JiraIssueData firstIssueInListData = null;

    for (String issueId : jiraTaskNGParameters.getUpdateIssueIds()) {
      try {
        Issue issue = jiraClient.getIssue(issueId);

        if (!issue.getProject().getKey().equals(jiraTaskNGParameters.getProject())) {
          return JiraTaskNGResponse.builder()
              .errorMessage(String.format(
                  "Provided issue identifier: \"%s\" does not correspond to Project: \"%s\". Please, provide valid key or id.",
                  issueId, jiraTaskNGParameters.getProject()))
              .executionStatus(FAILURE)
              .build();
        }

        boolean fieldsUpdated = false;
        FluentUpdate update = issue.update();

        if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getSummary())) {
          update.field(Field.SUMMARY, jiraTaskNGParameters.getSummary());
          fieldsUpdated = true;
        }

        if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getPriority())) {
          update.field(Field.PRIORITY, jiraTaskNGParameters.getPriority());
          fieldsUpdated = true;
        }

        if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getDescription())) {
          update.field(Field.DESCRIPTION, jiraTaskNGParameters.getDescription());
          fieldsUpdated = true;
        }

        if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getLabels())) {
          update.field(Field.LABELS, jiraTaskNGParameters.getLabels());
          fieldsUpdated = true;
        }

        if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getCustomFields())) {
          setCustomFieldsOnUpdate(jiraTaskNGParameters, update);
          fieldsUpdated = true;
        }

        if (fieldsUpdated) {
          update.execute();
        }

        if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getComment())) {
          issue.addComment(jiraTaskNGParameters.getComment());
        }

        if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getStatus())) {
          updateStatus(issue, jiraTaskNGParameters.getStatus());
        }

        log.info("Successfully updated ticket : " + issueId);
        issueKeys.add(issue.getKey());
        issueUrls.add(getIssueUrl(jiraTaskNGParameters.getJiraConnectorDTO(), issue.getKey()));

        if (firstIssueInListData == null) {
          firstIssueInListData = JiraIssueData.builder().description(jiraTaskNGParameters.getDescription()).build();
        }
      } catch (JiraException j) {
        String errorMessage = "Failed to update Jira Issue for Id: " + issueId + ". " + extractResponseMessage(j);
        log.error(errorMessage, j);
        return JiraTaskNGResponse.builder().errorMessage(errorMessage).executionStatus(FAILURE).build();
      } catch (WingsException we) {
        return JiraTaskNGResponse.builder()
            .errorMessage(ExceptionUtils.getMessage(we))
            .executionStatus(FAILURE)
            .build();
      }
    }

    final String regex = "[\\[\\]]";
    return JiraTaskNGResponse.builder()
        .executionStatus(SUCCESS)
        .errorMessage("Updated Jira ticket " + issueKeys.toString().replaceAll(regex, ""))
        .issueUrl(issueUrls.toString().replaceAll(regex, ""))
        .issueId(jiraTaskNGParameters.getUpdateIssueIds().get(0))
        .issueKey(issueKeys.toString().replaceAll(regex, ""))
        .jiraIssueData(firstIssueInListData)
        .build();
  }

  public JiraTaskNGResponse fetchIssue(JiraTaskNGParameters jiraTaskNGParameters) {
    JiraConnectorDTO jiraConnectorDTO = jiraTaskNGParameters.getJiraConnectorDTO();
    JiraClient jira;
    Issue issue;
    String approvalFieldValue = null;
    try {
      jira = getJiraClient(jiraTaskNGParameters);
      issue = jira.getIssue(jiraTaskNGParameters.getIssueId());
      String message = "Waiting for Approval on ticket: " + issue.getKey();
      if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getApprovalField())) {
        Map<String, String> fieldMap = (Map<String, String>) issue.getField(jiraTaskNGParameters.getApprovalField());
        approvalFieldValue = fieldMap.get(JIRA_APPROVAL_FIELD_KEY);
      }

      return JiraTaskNGResponse.builder()
          .executionStatus(RUNNING)
          .issueUrl(getIssueUrl(jiraConnectorDTO, issue.getKey()))
          .issueKey(issue.getKey())
          .currentStatus(approvalFieldValue)
          .errorMessage(message)
          .jiraIssueData(JiraIssueData.builder().description(issue.getDescription()).build())
          .build();
    } catch (JiraException e) {
      String error =
          "Unable to fetch Jira Issue for Id: " + jiraTaskNGParameters.getIssueId() + "  " + extractResponseMessage(e);
      log.error(error, e);
      return JiraTaskNGResponse.builder().executionStatus(FAILURE).errorMessage(error).build();
    }
  }

  public JiraTaskNGResponse getProjects(JiraTaskNGParameters jiraTaskNGParameters) {
    try {
      JiraClient jiraClient = getJiraClient(jiraTaskNGParameters);
      List<Project> projects = jiraClient.getProjects();
      List<JiraProjectData> jiraProjectDataList = new ArrayList<>();
      for (Project project : projects) {
        jiraProjectDataList.add(new JiraProjectData(project));
      }
      return JiraTaskNGResponse.builder().executionStatus(SUCCESS).projects(jiraProjectDataList).build();
    } catch (JiraException e) {
      String errorMessage = "Failed to fetch projects during credential validation.";
      log.error(errorMessage, e);
      return JiraTaskNGResponse.builder().errorMessage(errorMessage).executionStatus(FAILURE).build();
    }
  }

  public JiraTaskNGResponse getStatuses(JiraTaskNGParameters jiraTaskNGParameters) {
    try {
      JiraClient jiraClient = getJiraClient(jiraTaskNGParameters);
      URI uri = jiraClient.getRestClient().buildURI(
          Resource.getBaseUri() + "project/" + jiraTaskNGParameters.getProject() + "/statuses");
      JSON response = jiraClient.getRestClient().get(uri);

      List<JiraIssueType> issueTypeStatuses = new ArrayList<>();
      ((JSONArray) response).forEach(r -> issueTypeStatuses.add(new JiraIssueType((JSONObject) r)));

      return JiraTaskNGResponse.builder().executionStatus(SUCCESS).statuses(issueTypeStatuses).build();
    } catch (URISyntaxException | RestException | IOException | JiraException | RuntimeException e) {
      String errorMessage = "Failed to fetch statuses from Jira server.";
      log.error(errorMessage, e);
      return JiraTaskNGResponse.builder().errorMessage(errorMessage).executionStatus(FAILURE).build();
    }
  }

  public JiraTaskNGResponse getFieldsOptions(JiraTaskNGParameters jiraTaskNGParameters) {
    URI uri;
    Issue.SearchResult issues;
    JiraClient jiraClient;
    try {
      jiraClient = getJiraClient(jiraTaskNGParameters);
      String jqlQuery = "project = " + jiraTaskNGParameters.getProject();
      issues = jiraClient.searchIssues(jqlQuery, 1);
    } catch (JiraException | RuntimeException e) {
      String errorMessage =
          "Failed to fetch issues from Jira server for project - " + jiraTaskNGParameters.getProject();
      log.error(errorMessage, e);
      return JiraTaskNGResponse.builder().errorMessage(errorMessage).executionStatus(FAILURE).build();
    }

    Issue issue = null;
    if (CollectionUtils.isNotEmpty(issues.issues)) {
      issue = issues.issues.get(0);
    }
    String issueKey = (issue == null) ? (jiraTaskNGParameters.getProject() + "-1") : issue.getKey();

    try {
      uri = jiraClient.getRestClient().buildURI(Resource.getBaseUri() + "issue/" + issueKey + "/editmeta");
      JSON response = jiraClient.getRestClient().get(uri);

      List<JiraField> jiraFields = new ArrayList<>();

      JSONObject jsonFields = ((JSONObject) response).getJSONObject("fields");
      jsonFields.keySet().forEach(keyStr -> {
        String kk = (String) keyStr;
        JSONObject fieldData = jsonFields.getJSONObject(kk);
        jiraFields.add(new JiraField(fieldData, kk));
      });

      return JiraTaskNGResponse.builder().fields(jiraFields).executionStatus(SUCCESS).build();
    } catch (URISyntaxException | IOException | RestException | RuntimeException e) {
      String errorMessage = "Failed to fetch editmeta from Jira server. Issue - " + issueKey;
      log.error(errorMessage, e);
      return JiraTaskNGResponse.builder().errorMessage(errorMessage).executionStatus(FAILURE).build();
    }
  }

  public JiraTaskNGResponse checkJiraApproval(JiraTaskNGParameters jiraTaskNGParameters) {
    Issue issue;
    log.info("Checking approval for IssueId = {}", jiraTaskNGParameters.getIssueId());
    try {
      JiraClient jira = getJiraClient(jiraTaskNGParameters);
      issue = jira.getIssue(jiraTaskNGParameters.getIssueId());
    } catch (JiraException e) {
      String errorMessage = "Encoutered a problem while fetching Jira Issue " + jiraTaskNGParameters.getIssueId();
      if (e.getCause() != null) {
        errorMessage += " Reason: " + e.getCause().getMessage();
        if (e.getCause() instanceof RestException && ((RestException) e.getCause()).getHttpStatusCode() == 404) {
          // Fail if the jira issue is not returned
          log.error(errorMessage, e);
          return JiraTaskNGResponse.builder().executionStatus(FAILURE).errorMessage(errorMessage).build();
        }
      }
      log.error(errorMessage, e);
      return JiraTaskNGResponse.builder().executionStatus(RUNNING).errorMessage(errorMessage).build();
    }

    log.info("Issue fetched successfully for {}", jiraTaskNGParameters.getIssueId());
    String approvalFieldValue = null;
    if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getApprovalField())) {
      Map<String, String> fieldMap = (Map<String, String>) issue.getField(jiraTaskNGParameters.getApprovalField());
      approvalFieldValue = fieldMap.get(JIRA_APPROVAL_FIELD_KEY);
    }

    String rejectionFieldValue = null;
    if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getRejectionField())) {
      Map<String, String> fieldMap = (Map<String, String>) issue.getField(jiraTaskNGParameters.getRejectionField());
      rejectionFieldValue = fieldMap.get(JIRA_APPROVAL_FIELD_KEY);
    }

    log.info("IssueId: {}, approvalField: {}, approvalFieldValue: {}, rejectionField: {}, rejectionFieldValue: {}",
        jiraTaskNGParameters.getIssueId(), jiraTaskNGParameters.getApprovalField(), approvalFieldValue,
        jiraTaskNGParameters.getRejectionField(), rejectionFieldValue);

    if (EmptyPredicate.isNotEmpty(approvalFieldValue)
        && StringUtils.equalsIgnoreCase(approvalFieldValue, jiraTaskNGParameters.getApprovalValue())) {
      log.info("IssueId: {} Approved", jiraTaskNGParameters.getIssueId());
      return JiraTaskNGResponse.builder().currentStatus(approvalFieldValue).executionStatus(SUCCESS).build();
    }

    if (EmptyPredicate.isNotEmpty(rejectionFieldValue)
        && StringUtils.equalsIgnoreCase(rejectionFieldValue, jiraTaskNGParameters.getRejectionValue())) {
      log.info("IssueId: {} Rejected", jiraTaskNGParameters.getIssueId());
      return JiraTaskNGResponse.builder().currentStatus(rejectionFieldValue).executionStatus(FAILURE).build();
    }

    return JiraTaskNGResponse.builder().currentStatus(approvalFieldValue).executionStatus(RUNNING).build();
  }

  public JiraTaskNGResponse getCreateMetadata(JiraTaskNGParameters jiraTaskNGParameters) {
    URI uri;
    try {
      log.info("Getting decrypted jira client configs for GET_CREATE_METADATA");
      JiraClient jiraClient = getJiraClient(jiraTaskNGParameters);

      log.info("Building URI for GET_CREATE_METADATA");
      Map<String, String> queryParams = new HashMap<>();
      if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getCreatemetaExpandParam())) {
        queryParams.put("expand", jiraTaskNGParameters.getCreatemetaExpandParam());
      } else {
        queryParams.put("expand", "projects.issuetypes.fields");
      }

      if (EmptyPredicate.isNotEmpty(jiraTaskNGParameters.getProject())) {
        queryParams.put("projectKeys", jiraTaskNGParameters.getProject());
      }

      uri = jiraClient.getRestClient().buildURI(Resource.getBaseUri() + "issue/createmeta", queryParams);

      log.info(" Fetching metadata from jira for GET_CREATE_METADATA");
      JSON response = jiraClient.getRestClient().get(uri);

      log.info(" Response received from jira for GET_CREATE_METADATA");
      JiraCreateMetaResponse jiraCreateMetaResponse = new JiraCreateMetaResponse((JSONObject) response);

      log.info(" Fetching resolutions from jira for GET_CREATE_METADATA");
      URI resolutionUri = jiraClient.getRestClient().buildURI(Resource.getBaseUri() + RESOLUTION);
      JSONArray resolutions = (JSONArray) jiraClient.getRestClient().get(resolutionUri);
      insertResolutionsInCreateMeta(resolutions, jiraCreateMetaResponse);

      log.info(" Returning response to manager for GET_CREATE_METADATA");
      return JiraTaskNGResponse.builder().executionStatus(SUCCESS).createMetadata(jiraCreateMetaResponse).build();
    } catch (URISyntaxException | RestException | IOException | JiraException | RuntimeException e) {
      String errorMessage = "Failed to fetch issue metadata from Jira server.";
      log.error(errorMessage, e);
      return JiraTaskNGResponse.builder().errorMessage(errorMessage).executionStatus(FAILURE).build();
    }
  }

  public void updateStatus(Issue issue, String status) throws JiraException {
    List<Transition> allTransitions = null;
    try {
      allTransitions = issue.getTransitions(); // gives all transitions available for that issue
    } catch (JiraException e) {
      log.error("Failed to get all transitions from the Jira");
      throw e;
    }

    Transition transition =
        allTransitions.stream().filter(t -> t.getToStatus().getName().equalsIgnoreCase(status)).findAny().orElse(null);
    if (transition != null) {
      try {
        issue.transition().execute(transition);
      } catch (JiraException e) {
        log.error("Exception while trying to update status to {}", status);
        throw e;
      }
    } else {
      log.error("No transition found from {} to {}", issue.getStatus(), status);
      throw new JiraException("No transition found from [" + issue.getStatus() + "] to [" + status + "]");
    }
  }

  void setCustomFieldsOnCreate(JiraTaskNGParameters parameters, FluentCreate fluentCreate) {
    TimeTracking timeTracking = new TimeTracking();
    for (Map.Entry<String, JiraCustomFieldValue> customField : parameters.getCustomFields().entrySet()) {
      if (customField.getKey().equals(ORIGINAL_ESTIMATE)) {
        timeTracking.setOriginalEstimate((String) getCustomFieldValue(customField));
      } else if (customField.getKey().equals(REMAINING_ESTIMATE)) {
        timeTracking.setRemainingEstimate((String) getCustomFieldValue(customField));
      } else {
        fluentCreate.field(customField.getKey(), getCustomFieldValue(customField));
      }
    }
    if (timeTracking.getOriginalEstimate() != null || timeTracking.getRemainingEstimate() != null) {
      fluentCreate.field(Field.TIME_TRACKING, timeTracking);
    }
  }

  void setCustomFieldsOnUpdate(JiraTaskNGParameters parameters, FluentUpdate update) {
    TimeTracking timeTracking = new TimeTracking();
    for (Map.Entry<String, JiraCustomFieldValue> customField : parameters.getCustomFields().entrySet()) {
      if (customField.getKey().equals(ORIGINAL_ESTIMATE)) {
        timeTracking.setOriginalEstimate((String) getCustomFieldValue(customField));
      } else if (customField.getKey().equals(REMAINING_ESTIMATE)) {
        timeTracking.setRemainingEstimate((String) getCustomFieldValue(customField));
      } else {
        update.field(customField.getKey(), getCustomFieldValue(customField));
      }
    }
    if (timeTracking.getOriginalEstimate() != null || timeTracking.getRemainingEstimate() != null) {
      update.field(Field.TIME_TRACKING, timeTracking);
    }
  }

  private JiraClient getJiraClient(JiraTaskNGParameters parameters) throws JiraException {
    JiraConnectorDTO jiraConnectorDTO = parameters.getJiraConnectorDTO();
    BasicCredentials creds = new BasicCredentials(
        jiraConnectorDTO.getUsername(), String.valueOf(jiraConnectorDTO.getPasswordRef().getDecryptedValue()));
    String jiraUrl = jiraConnectorDTO.getJiraUrl();

    String baseUrl = jiraUrl.endsWith("/") ? jiraUrl : jiraUrl.concat("/");
    return new JiraClient(baseUrl, creds);
  }

  private Object getCustomFieldValue(Map.Entry<String, JiraCustomFieldValue> customFieldValueEntry) {
    String fieldName = customFieldValueEntry.getKey();
    String type = customFieldValueEntry.getValue().getFieldType();
    String fieldValue = customFieldValueEntry.getValue().getFieldValue();

    switch (type) {
      case "option":
      case RESOLUTION: {
        return new ValueTuple("id", fieldValue);
      }
      case "number":
        return Double.parseDouble(fieldValue);
      case "date":
      case "string":
      case "any":
        return fieldValue;
      case TIME_TRACKING:
        return fieldValue.replace(" ", "").replace("w", "w ").replace("d", "d ").replace("h", "h ").trim();
      case "datetime":
        return new Timestamp(Long.parseLong(fieldValue));
      case "multiselect":
        List<ValueTuple> valueTuples = new ArrayList<>();
        List<String> valueList = Arrays.asList(fieldValue.split(","));
        for (String value : valueList) {
          ValueTuple valueTuple = new ValueTuple("id", value);
          valueTuples.add(valueTuple);
        }
        return valueTuples;
      case "array":
        return Arrays.asList(fieldValue.split(" "));

      default:
        throw new InvalidRequestException("FieldType " + type + "not supported in Harness for " + fieldName);
    }
  }

  private String getIssueUrl(JiraConnectorDTO connectorDTO, String issueKey) {
    try {
      URL issueUrl = new URL(
          connectorDTO.getJiraUrl() + (connectorDTO.getJiraUrl().endsWith("/") ? "" : "/") + "browse/" + issueKey);

      return issueUrl.toString();
    } catch (MalformedURLException e) {
      log.info("Incorrect url: " + e.getMessage());
    }

    return null;
  }

  private String extractResponseMessage(Exception e) {
    if (e.getCause() != null) {
      String messageJson = "{" + e.getCause().getMessage() + "}";
      org.json.JSONObject jsonObject;
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
        return errorsKey + " : " + errors.get(errorsKey);
      } catch (Exception ex) {
        log.error("Failed to parse json response from Jira", ex);
      }
    }

    return e.getMessage();
  }

  private void insertResolutionsInCreateMeta(JSONArray resolutions, JiraCreateMetaResponse jiraCreateMetaResponse) {
    Map<String, Object> resolutionProperties = new HashMap<>();
    Map<String, String> schema = new HashMap<>();
    schema.put("type", RESOLUTION);
    schema.put("system", RESOLUTION);
    resolutionProperties.put("schema", schema);
    resolutionProperties.put("required", "false");
    resolutionProperties.put("key", RESOLUTION);
    resolutionProperties.put("name", "Resolution");
    resolutionProperties.put("allowedValues", resolutions);

    JSONObject resolutionObject = JSONObject.fromObject(resolutionProperties);

    JiraField resolution = JiraField.getNewField(resolutionObject, RESOLUTION);

    jiraCreateMetaResponse.getProjects().forEach(jiraProjectData
        -> jiraProjectData.getIssueTypes().forEach(
            jiraIssueType -> jiraIssueType.getJiraFields().put(RESOLUTION, resolution)));
  }
}
