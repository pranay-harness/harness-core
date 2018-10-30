package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.List;
/**
 * Created by anubhaw on 10/16/17.
 */

/**
 * The type Git diff result.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class GitDiffResult extends GitCommandResult {
  private String repoName;
  private String branch;
  private String commitId;
  private List<GitFileChange> gitFileChanges = new ArrayList<>();
  private YamlGitConfig yamlGitConfig;

  public GitDiffResult() {
    super(GitCommandType.DIFF);
  }

  public GitDiffResult(String repoName, String branch, String commitId, List<GitFileChange> gitFileChanges,
      YamlGitConfig yamlGitConfig) {
    super(GitCommandType.DIFF);
    this.repoName = repoName;
    this.branch = branch;
    this.commitId = commitId;
    this.gitFileChanges = gitFileChanges;
    this.yamlGitConfig = yamlGitConfig;
  }

  /**
   * Add change file.
   *
   * @param gitFileChange the git file change
   */
  public void addChangeFile(GitFileChange gitFileChange) {
    gitFileChanges.add(gitFileChange);
  }
}
