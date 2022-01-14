package software.wings.beans.yaml;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.DX)
@TargetModule(HarnessModule._970_API_SERVICES_BEANS)
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
