package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class GitFetchFilesRequest extends GitCommandRequest {
  private String commitId;
  private List<String> filePaths;
  private String branch;
  private String gitConnectorId;
  private boolean useBranch;

  public GitFetchFilesRequest() {
    super(GitCommandType.FETCH_FILES);
  }

  public GitFetchFilesRequest(
      String commitId, List<String> filePaths, String branch, String gitConnectorId, boolean useBranch) {
    super(GitCommandType.FETCH_FILES);
    this.commitId = commitId;
    this.filePaths = filePaths;
    this.branch = branch;
    this.gitConnectorId = gitConnectorId;
    this.useBranch = useBranch;
  }
}
