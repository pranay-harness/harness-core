package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(CDC)
public class ShellScriptApprovalParams {
  @Getter @Setter private String scriptString;

  /* Retry Interval in Milliseconds*/
  @Getter @Setter private Integer retryInterval;
}
