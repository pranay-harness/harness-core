package software.wings.beans.yaml;

import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by anubhaw on 10/16/17.
 */

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GitCommitRequest extends GitCommandRequest {
  private List<GitFileChange> gitFileChanges;
  private boolean forcePush;
  private List<String> yamlChangeSetIds;
  private YamlGitConfig yamlGitConfig;
  String lastProcessedGitCommit;
  boolean pushOnlyIfHeadSeen;

  public GitCommitRequest() {
    super(GitCommandType.COMMIT);
  }

  public GitCommitRequest(List<GitFileChange> gitFileChanges, boolean forcePush, List<String> yamlChangeSetIds,
      YamlGitConfig yamlGitConfig, String lastProcessedGitCommit, boolean pushOnlyIfHeadSeen) {
    super(GitCommandType.COMMIT);
    this.gitFileChanges = gitFileChanges;
    this.forcePush = forcePush;
    this.yamlChangeSetIds = yamlChangeSetIds;
    this.yamlGitConfig = yamlGitConfig;
    this.lastProcessedGitCommit = lastProcessedGitCommit;
    this.pushOnlyIfHeadSeen = pushOnlyIfHeadSeen;
  }
}
