package io.harness.git.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CommitResult extends GitBaseResult {
  private String commitId;
  private int commitTime;
  private String commitMessage;

  @Builder
  public CommitResult(String accountId, String commitId, int commitTime, String commitMessage) {
    super(accountId);
    this.commitId = commitId;
    this.commitTime = commitTime;
    this.commitMessage = commitMessage;
  }
}
