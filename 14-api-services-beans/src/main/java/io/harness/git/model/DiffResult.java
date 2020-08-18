package io.harness.git.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DiffResult extends GitBaseResult {
  private String repoName;
  private String branch;
  private String commitId;
  private List<GitFileChange> gitFileChanges;
  private Long commitTimeMs;
  private String commitMessage;

  @Builder
  public DiffResult(String accountId, String repoName, String branch, String commitId,
      List<GitFileChange> gitFileChanges, Long commitTimeMs, String commitMessage) {
    super(accountId);
    this.repoName = repoName;
    this.branch = branch;
    this.commitId = commitId;
    this.gitFileChanges = gitFileChanges;
    this.commitTimeMs = commitTimeMs;
    this.commitMessage = commitMessage;
  }
}
