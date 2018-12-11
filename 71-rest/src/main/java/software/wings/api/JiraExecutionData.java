package software.wings.api;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;
import net.sf.json.JSONArray;
import software.wings.delegatetasks.jira.JiraAction;
import software.wings.sm.StateExecutionData;

import java.util.Map;

@Data
@Builder
public class JiraExecutionData extends StateExecutionData implements ResponseData {
  private EmbeddedUser approvedBy;
  private Long approvedOn;
  private String approvalId;

  private String activityId;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private JiraAction jiraAction;
  private String issueId;
  private JSONArray projects;
  private JSONArray fields;
  private String webhookUrl;
  private JiraApprovalActionType jiraApprovalActionType;

  public enum JiraApprovalActionType { CREATE_WEBHOOK, WAIT_JIRA_APPROVAL, DELETE_WEBHOOK }

  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "activityId", ExecutionDataValue.builder().displayName("").value(activityId).build());
    return executionDetails;
  }
}
