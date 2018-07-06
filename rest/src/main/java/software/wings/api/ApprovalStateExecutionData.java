package software.wings.api;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.EmbeddedUser;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class ApprovalStateExecutionData extends StateExecutionData implements NotifyResponseData {
  private EmbeddedUser approvedBy;
  private Long approvedOn;
  private String comments;
  private String approvalId;

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
    putNotNull(executionDetails, "approvalId",
        ExecutionDataValue.builder().displayName("Approval Id").value(approvalId).build());
    putNotNull(executionDetails, "status",
        ExecutionDataValue.builder().displayName("Approval Status").value(getStatus()).build());

    if (approvedBy != null) {
      StringBuilder approvedRejectedBy =
          new StringBuilder(approvedBy.getName()).append(" (").append(approvedBy.getEmail()).append(")");
      if (getStatus().equals(ExecutionStatus.SUCCESS)) {
        putNotNull(executionDetails, "approvedBy",
            ExecutionDataValue.builder().displayName("Approved By").value(approvedRejectedBy.toString()).build());
      } else if (getStatus().equals(ExecutionStatus.REJECTED)) {
        putNotNull(executionDetails, "approvedBy",
            ExecutionDataValue.builder().displayName("Rejected By").value(approvedRejectedBy.toString()).build());
      }
    }

    putNotNull(executionDetails, "approvedOn",
        ExecutionDataValue.builder().displayName("Approved On").value(approvedOn).build());
    putNotNull(
        executionDetails, "comments", ExecutionDataValue.builder().displayName("Comments").value(comments).build());
    return executionDetails;
  }
}
