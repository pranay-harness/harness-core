package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;

/**
 * Created by anubhaw on 10/16/17.
 */

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class GitCommitRequest extends GitCommandRequest {
  private List<GitFileChange> gitFileChanges;
  private boolean forcePush;
  private List<String> yamlChangeSetIds;
  private YamlGitConfig yamlGitConfig;

  public GitCommitRequest() {
    super(GitCommandType.COMMIT);
  }

  public GitCommitRequest(List<GitFileChange> gitFileChanges, boolean forcePush, List<String> yamlChangeSetIds,
      YamlGitConfig yamlGitConfig) {
    super(GitCommandType.COMMIT);
    this.gitFileChanges = gitFileChanges;
    this.forcePush = forcePush;
    this.yamlChangeSetIds = yamlChangeSetIds;
    this.yamlGitConfig = yamlGitConfig;
  }
}
