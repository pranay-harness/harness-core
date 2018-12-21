package software.wings.api;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import software.wings.delegatetasks.jira.JiraAction;
import software.wings.sm.StateExecutionData;

import java.util.Map;

@Data
@Builder
public class JiraExecutionData extends StateExecutionData implements ResponseData {
  private String activityId;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private JiraAction jiraAction;
  private String issueId;
  private String issueUrl;

  private JSONArray projects;
  private JSONObject fields;
  private JSONArray statuses;
  private JSONObject createMetadata;

  private String webhookUrl;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    return setExecutionData(executionDetails);
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    return setExecutionData(executionDetails);
  }

  private Map<String, ExecutionDataValue> setExecutionData(Map<String, ExecutionDataValue> executionDetails) {
    putNotNull(
        executionDetails, "issueUrl", ExecutionDataValue.builder().displayName("Issue Url").value(issueUrl).build());
    return executionDetails;
  }
}
