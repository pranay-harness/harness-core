package io.harness.gitsync.beans;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GitDiffResult extends GitCommandResult {
  private String repoName;
  private String branch;
  private String commitId;
  private List<GitFileChange> gitFileChanges = new ArrayList<>();
  // TODO(abhinav): Add something in lines of YamlGitConfig;
  private Long commitTimeMs;
  private String commitMessage;

  public GitDiffResult() {
    super(GitCommandType.DIFF);
  }

  public GitDiffResult(String repoName, String branch, String commitId, List<GitFileChange> gitFileChanges,
      Long commitTimeMs, String commitMessage) {
    super(GitCommandType.DIFF);
    this.repoName = repoName;
    this.branch = branch;
    this.commitId = commitId;
    this.gitFileChanges = gitFileChanges;
    this.commitTimeMs = commitTimeMs;
    this.commitMessage = commitMessage;
  }
}
