package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.yaml.gitSync.YamlGitConfig;

/**
 * Created by anubhaw on 10/16/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class GitCommitAndPushResult extends GitCommandResult {
  private GitCommitResult gitCommitResult;
  private GitPushResult gitPushResult;
  private YamlGitConfig yamlGitConfig;

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
  public GitCommitAndPushResult(
      GitCommitResult gitCommitResult, GitPushResult gitPushResult, YamlGitConfig yamlGitConfig) {
    super(GitCommandType.COMMIT_AND_PUSH);
    this.gitCommitResult = gitCommitResult;
    this.gitPushResult = gitPushResult;
    this.yamlGitConfig = yamlGitConfig;
  }
}
