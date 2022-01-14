package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@FieldNameConstants(innerTypeName = "ApprovalStateParamsKeys")
@TargetModule(HarnessModule._957_CG_BEANS)
public class ApprovalStateParams {
  @Getter @Setter private JiraApprovalParams jiraApprovalParams;
  @Getter @Setter private ShellScriptApprovalParams shellScriptApprovalParams;
  @Getter @Setter private ServiceNowApprovalParams serviceNowApprovalParams;
}
