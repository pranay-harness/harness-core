package software.wings.beans.approval;

import lombok.Getter;
import lombok.Setter;

public class ShellScriptApprovalParams {
  @Getter @Setter private String scriptString;

  /* Retry Interval in Milliseconds*/
  @Getter @Setter private Integer retryInterval;
}
