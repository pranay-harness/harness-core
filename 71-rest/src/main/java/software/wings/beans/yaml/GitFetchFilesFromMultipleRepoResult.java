package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GitFetchFilesFromMultipleRepoResult extends GitCommandResult {
  Map<String, GitFetchFilesResult> filesFromMultipleRepo;

  public GitFetchFilesFromMultipleRepoResult() {
    super(GitCommandType.FETCH_FILES_FROM_MULTIPLE_REPO);
  }

  public GitFetchFilesFromMultipleRepoResult(Map<String, GitFetchFilesResult> filesFromMultipleRepo) {
    super(GitCommandType.FETCH_FILES_FROM_MULTIPLE_REPO);
    this.filesFromMultipleRepo = filesFromMultipleRepo;
  }
}
