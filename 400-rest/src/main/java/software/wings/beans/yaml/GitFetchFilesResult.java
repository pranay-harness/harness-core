package software.wings.beans.yaml;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.git.model.GitFile;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GitFetchFilesResult extends GitCommandResult {
  private GitCommitResult gitCommitResult;
  private List<GitFile> files;

  public GitFetchFilesResult() {
    super(GitCommandType.FETCH_FILES);
  }

  public GitFetchFilesResult(GitCommitResult gitCommitResult, List<GitFile> gitFiles) {
    super(GitCommandType.FETCH_FILES);
    this.gitCommitResult = gitCommitResult;
    this.files = isEmpty(gitFiles) ? Collections.EMPTY_LIST : gitFiles;
  }
}
