package io.harness.delegate.beans.git;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GitCommitResult extends GitCommandResult {
  private String commitId;
  private int commitTime;
  private String commitMessage;

  /**
   * Instantiates a new Git commit result.
   */
  public GitCommitResult() {
    super(GitCommandType.COMMIT);
  }

  /**
   * Instantiates a new Git commit result.
   *
   * @param commitId   the commit id
   * @param commitTime the commit time
   */
  public GitCommitResult(String commitId, int commitTime, String commitMessage) {
    super(GitCommandType.COMMIT);
    this.commitId = commitId;
    this.commitTime = commitTime;
    this.commitMessage = commitMessage;
  }

  /**
   * Sets commit id.
   *
   * @param commitId the commit id
   */
  public void setCommitId(String commitId) {
    this.commitId = commitId;
  }
}
