package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GitFetchFilesRequest extends GitCommandRequest {
  private String commitId;
  private List<String> filePaths;
  private String branch;
  private String gitConnectorId;
  private boolean useBranch;
  private List<String> fileExtensions;
  private boolean recursive;

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

  public GitFetchFilesRequest(String commitId, List<String> filePaths, String branch, String gitConnectorId,
      boolean useBranch, List<String> fileExtensions, boolean recursive) {
    super(GitCommandType.FETCH_FILES);
    this.commitId = commitId;
    this.filePaths = filePaths;
    this.branch = branch;
    this.gitConnectorId = gitConnectorId;
    this.useBranch = useBranch;
    this.fileExtensions = fileExtensions;
    this.recursive = recursive;
  }
}
