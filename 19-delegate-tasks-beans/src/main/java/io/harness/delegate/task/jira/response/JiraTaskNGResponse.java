package io.harness.delegate.task.jira.response;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.jira.JiraAction;
import io.harness.jira.JiraCreateMetaResponse;
import io.harness.jira.JiraProjectData;
import io.harness.logging.CommandExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class JiraTaskNGResponse implements DelegateTaskNotifyResponseData {
  private String activityId;
  private CommandExecutionStatus executionStatus;
  private String errorMessage;
  private JiraAction jiraAction;
  private String issueId;
  private String issueKey;
  private String issueUrl;
  private String jiraServerResponse;

  private List<JiraProjectData> projects;
  private JSONObject fields;
  private JSONArray statuses;
  private JiraCreateMetaResponse createMetadata;

  private String currentStatus;

  private JiraIssueData jiraIssueData;

  private DelegateMetaInfo delegateMetaInfo;

  @Data
  @Builder
  public static class JiraIssueData {
    private String description;
  }
}