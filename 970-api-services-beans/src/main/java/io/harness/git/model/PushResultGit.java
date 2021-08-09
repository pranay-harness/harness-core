package io.harness.git.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PushResultGit extends GitBaseResult {
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

  @Builder(builderMethodName = "pushResultBuilder")
  public PushResultGit(String accountId, RefUpdate refUpdate) {
    super(accountId);
    this.refUpdate = refUpdate;
  }
}
