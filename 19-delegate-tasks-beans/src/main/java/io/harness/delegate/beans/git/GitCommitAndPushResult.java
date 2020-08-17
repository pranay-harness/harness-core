package io.harness.delegate.beans.git;

import io.harness.git.model.GitFileChange;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GitCommitAndPushResult extends GitCommandResult {
  private GitCommitResult gitCommitResult;
  private GitPushResult gitPushResult;
  private YamlGitConfigDTO yamlGitConfig;
  private List<GitFileChange> filesCommitedToGit;

  /**
   * Instantiates a new Git commit and push result.
   */
  public GitCommitAndPushResult() {
    super(GitCommandType.COMMIT_AND_PUSH);
  }

  /**
   * Instantiates a new Git commit and push result.
   *
   * @param gitCommitResult the git commit result
   * @param gitPushResult   the git push result
   */
  public GitCommitAndPushResult(GitCommitResult gitCommitResult, GitPushResult gitPushResult,
      YamlGitConfigDTO yamlGitConfig, List<GitFileChange> filesCommitedToGit) {
    super(GitCommandType.COMMIT_AND_PUSH);
    this.gitCommitResult = gitCommitResult;
    this.gitPushResult = gitPushResult;
    this.yamlGitConfig = yamlGitConfig;
    this.filesCommitedToGit = filesCommitedToGit;
  }
}