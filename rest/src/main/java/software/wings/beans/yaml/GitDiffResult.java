package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
/**
 * Created by anubhaw on 10/16/17.
 */

/**
 * The type Git diff result.
 */
@Data
@Builder
public class GitDiffResult extends GitCommandResult {
  private String repoName;
  private String branch;
  private String commitId;
  private List<GitFileChange> gitFileChanges = new ArrayList<>();

  public GitDiffResult() {
    super(GitCommandType.DIFF);
  }

  public GitDiffResult(String repoName, String branch, String commitId, List<GitFileChange> gitFileChanges) {
    super(GitCommandType.DIFF);
    this.repoName = repoName;
    this.branch = branch;
    this.commitId = commitId;
    this.gitFileChanges = gitFileChanges;
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
