package io.harness.delegate.beans.git;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GitCommitAndPushRequest extends GitCommandRequest {
  private List<GitFileChange> gitFileChanges;
  private boolean forcePush;
  private String yamlChangeSetId;
  private List<YamlGitConfigDTO> yamlGitConfigs;

  public GitCommitAndPushRequest() {
    super(GitCommandType.COMMIT_AND_PUSH);
  }

  public GitCommitAndPushRequest(List<GitFileChange> gitFileChanges, boolean forcePush, String yamlChangeSetId,
      List<YamlGitConfigDTO> yamlGitConfigs) {
    super(GitCommandType.COMMIT_AND_PUSH);
    this.gitFileChanges = gitFileChanges;
    this.forcePush = forcePush;
    this.yamlChangeSetId = yamlChangeSetId;
    this.yamlGitConfigs = yamlGitConfigs;
  }
}