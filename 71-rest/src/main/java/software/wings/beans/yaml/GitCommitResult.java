package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by anubhaw on 10/16/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class GitCommitResult extends GitCommandResult {
  private String commitId;
  private int commitTime;

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
  public GitCommitResult(String commitId, int commitTime) {
    super(GitCommandType.COMMIT);
    this.commitId = commitId;
    this.commitTime = commitTime;
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
