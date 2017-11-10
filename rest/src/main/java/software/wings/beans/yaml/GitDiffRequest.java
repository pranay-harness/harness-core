package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;

/**
 * Created by anubhaw on 11/2/17.
 */
@Data
@Builder

public class GitDiffRequest extends GitCommandRequest {
  private String lastProcessedCommitId;

  public GitDiffRequest() {
    super(GitCommandType.DIFF);
  }

  public GitDiffRequest(String lastProcessedCommitId) {
    super(GitCommandType.DIFF);
    this.lastProcessedCommitId = lastProcessedCommitId;
  }
}
