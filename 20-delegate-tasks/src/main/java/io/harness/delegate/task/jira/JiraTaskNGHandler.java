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
import io.harness.jira.JiraCustomFieldValue;
import lombok.extern.slf4j.Slf4j;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Field.ValueTuple;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Issue.FluentCreate;
import net.rcarz.jiraclient.Issue.FluentUpdate;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.TimeTracking;
import net.rcarz.jiraclient.Transition;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static net.rcarz.jiraclient.Field.RESOLUTION;
import static net.rcarz.jiraclient.Field.TIME_TRACKING;

@Singleton
@Slf4j
public class JiraTaskNGHandler {
  @VisibleForTesting static final String ORIGINAL_ESTIMATE = "TimeTracking:OriginalEstimate";
  @VisibleForTesting static final String REMAINING_ESTIMATE = "TimeTracking:RemainingEstimate";

  public JiraTaskNGResponse validateCredentials(JiraTaskNGParameters jiraTaskNGParameters) {
    try {
      JiraClient jiraClient = getJiraClient(jiraTaskNGParameters);
      jiraClient.getProjects();
    } catch (JiraException e) {
      String errorMessage = "Failed to fetch projects during credential validation.";
      logger.error(errorMessage, e);
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

      logger.info("Script execution finished with status SUCCESS");

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
      logger.error(errorMessage, e);
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

        logger.info("Successfully updated ticket : " + issueId);
        issueKeys.add(issue.getKey());
        issueUrls.add(getIssueUrl(jiraTaskNGParameters.getJiraConnectorDTO(), issue.getKey()));

        if (firstIssueInListData == null) {
          firstIssueInListData = JiraIssueData.builder().description(jiraTaskNGParameters.getDescription()).build();
        }
      } catch (JiraException j) {
        String errorMessage = "Failed to update Jira Issue for Id: " + issueId + ". " + extractResponseMessage(j);
        logger.error(errorMessage, j);
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

  private JiraClient getJiraClient(JiraTaskNGParameters jiraTaskNGParameters) throws JiraException {
    JiraConnectorDTO jiraConnectorDTO = jiraTaskNGParameters.getJiraConnectorDTO();
    BasicCredentials creds = new BasicCredentials(
        jiraConnectorDTO.getUsername(), String.valueOf(jiraConnectorDTO.getPasswordRef().getDecryptedValue()));
    String jiraUrl = jiraConnectorDTO.getJiraUrl();

    String baseUrl = jiraUrl.endsWith("/") ? jiraUrl : jiraUrl.concat("/");
    return new JiraClient(baseUrl, creds);
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
      logger.info("Incorrect url: " + e.getMessage());
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
        logger.error("Failed to parse json response from Jira", ex);
      }
    }

    return e.getMessage();
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
}