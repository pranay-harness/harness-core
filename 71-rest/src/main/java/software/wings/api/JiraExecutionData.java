package software.wings.api;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;
import net.sf.json.JSONArray;
import software.wings.delegatetasks.jira.JiraAction;
import software.wings.sm.ExecutionStatus;
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
  private JSONArray projects;
  private JSONArray fields;

  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "activityId", ExecutionDataValue.builder().displayName("").value(activityId).build());
    return executionDetails;
  }
}
