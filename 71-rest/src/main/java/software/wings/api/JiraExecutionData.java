package software.wings.api;

import io.harness.beans.EmbeddedUser;
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
  private EmbeddedUser approvedBy;
  private Long approvedOn;
  private String approvalId;

  private String activityId;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private JiraAction jiraAction;
  private String issueId;
  private String issueUrl;
  private JSONArray projects;
  private JSONObject fields;
  private JSONArray statuses;
  private String webhookUrl;
  private JiraApprovalActionType jiraApprovalActionType;

  public enum JiraApprovalActionType { CREATE_WEBHOOK, WAIT_JIRA_APPROVAL, DELETE_WEBHOOK }

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
    if (jiraApprovalActionType != null) {
      putNotNull(executionDetails, "status",
          ExecutionDataValue.builder().displayName("Approval Status").value(getStatus()).build());
    }

    if (approvedBy != null) {
      StringBuilder approvedRejectedBy =
          new StringBuilder(approvedBy.getName()).append(" (").append(approvedBy.getEmail()).append(")");
      if (getStatus().equals(ExecutionStatus.SUCCESS)) {
        putNotNull(executionDetails, "approvedBy",
            ExecutionDataValue.builder().displayName("Approved By").value(approvedRejectedBy.toString()).build());
      }
    }

    putNotNull(executionDetails, "approvedOn",
        ExecutionDataValue.builder().displayName("Approved On").value(approvedOn).build());
    return executionDetails;
  }
}
