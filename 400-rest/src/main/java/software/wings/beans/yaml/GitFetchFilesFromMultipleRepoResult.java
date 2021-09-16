package software.wings.beans.yaml;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._870_YAML_BEANS)
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
