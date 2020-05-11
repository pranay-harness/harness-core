package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(CDC)
public class ApprovalStateParams {
  @Getter @Setter private JiraApprovalParams jiraApprovalParams;
  @Getter @Setter private ShellScriptApprovalParams shellScriptApprovalParams;
  @Getter @Setter private ServiceNowApprovalParams serviceNowApprovalParams;
}
