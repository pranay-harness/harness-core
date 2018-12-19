package software.wings.beans.approval;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

public class JiraApprovalParams {
  @Getter @Setter @NotNull String jiraConnectorId;
  @Getter @Setter private String approvalField;
  @Getter @Setter private String approvalValue;
  @Getter @Setter private String rejectionField;
  @Getter @Setter private String rejectionValue;
  @Getter @Setter private String issueId;
}
