package io.harness.git.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CommitAndPushResult extends GitBaseResult {
  private CommitResult gitCommitResult;
  private PushResultGit gitPushResult;
  private List<GitFileChange> filesCommittedToGit;

  @Builder
  public CommitAndPushResult(String accountId, CommitResult gitCommitResult, PushResultGit gitPushResult,
      List<GitFileChange> filesCommittedToGit) {
    super(accountId);
    this.gitCommitResult = gitCommitResult;
    this.gitPushResult = gitPushResult;
    this.filesCommittedToGit = filesCommittedToGit;
  }
}
