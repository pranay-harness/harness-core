package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by anubhaw on 10/16/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class GitPushResult extends GitCommandResult {
  public GitPushResult() {
    super(GitCommandType.PUSH);
  }

  public GitPushResult(RefUpdate refUpdate) {
    super(GitCommandType.PUSH);
    this.refUpdate = refUpdate;
  }

  private RefUpdate refUpdate;

  @Data
  @Builder
  public static class RefUpdate {
    private String expectedOldObjectId;
    private final String newObjectId;
    private boolean forceUpdate;
    private String status;
    private String message;
  }
}
