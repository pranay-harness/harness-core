package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
public class JiraApprovalParams {
  @Getter @Setter @NotNull String jiraConnectorId;
  @Getter @Setter private String approvalField;
  @Getter @Setter private String approvalValue;
  @Getter @Setter private String rejectionField;
  @Getter @Setter private String rejectionValue;
  @Getter @Setter private String issueId;
  @Getter @Setter private String project;
}
