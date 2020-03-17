package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.yaml.gitSync.YamlGitConfig;

/**
 * Created by anubhaw on 11/2/17.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GitDiffRequest extends GitCommandRequest {
  private String lastProcessedCommitId;
  private YamlGitConfig yamlGitConfig;
  private String endCommitId;

  public GitDiffRequest() {
    super(GitCommandType.DIFF);
  }

  public GitDiffRequest(String lastProcessedCommitId, YamlGitConfig yamlGitConfig, String endCommitId) {
    super(GitCommandType.DIFF);
    this.lastProcessedCommitId = lastProcessedCommitId;
    this.yamlGitConfig = yamlGitConfig;
    this.endCommitId = endCommitId;
  }
}
