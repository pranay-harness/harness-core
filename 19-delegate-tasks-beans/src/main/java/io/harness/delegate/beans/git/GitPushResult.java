package io.harness.delegate.beans.git;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
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