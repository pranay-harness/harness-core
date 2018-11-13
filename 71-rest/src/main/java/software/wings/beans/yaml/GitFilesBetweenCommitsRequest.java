package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class GitFilesBetweenCommitsRequest extends GitCommandRequest {
  private String newCommitId;
  private String oldCommitId;
  private String gitConnectorId;

  public GitFilesBetweenCommitsRequest() {
    super(GitCommandType.FILES_BETWEEN_COMMITS);
  }

  public GitFilesBetweenCommitsRequest(String newCommitId, String oldCommitId, String gitConnectorId) {
    super(GitCommandType.FILES_BETWEEN_COMMITS);
    this.newCommitId = newCommitId;
    this.oldCommitId = oldCommitId;
    this.gitConnectorId = gitConnectorId;
  }
}
