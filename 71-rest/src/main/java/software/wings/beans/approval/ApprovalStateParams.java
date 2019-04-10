package software.wings.beans.approval;

import lombok.Getter;
import lombok.Setter;

public class ApprovalStateParams {
  @Getter @Setter private JiraApprovalParams jiraApprovalParams;
  @Getter @Setter private ShellScriptApprovalParams shellScriptApprovalParams;
  @Getter @Setter private ServiceNowApprovalParams serviceNowApprovalParams;
}
