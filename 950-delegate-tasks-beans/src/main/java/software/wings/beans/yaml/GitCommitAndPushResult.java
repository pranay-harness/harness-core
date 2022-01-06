/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
public class GitCommitAndPushResult extends GitCommandResult {
  private GitCommitResult gitCommitResult;
  private GitPushResult gitPushResult;
  private YamlGitConfig yamlGitConfig;
  private List<GitFileChange> filesCommitedToGit;

  /**
   * Instantiates a new Git commit and push result.
   */
  public GitCommitAndPushResult() {
    super(GitCommandType.COMMIT_AND_PUSH);
  }

  /**
   * Instantiates a new Git commit and push result.
   *
   * @param gitCommitResult the git commit result
   * @param gitPushResult   the git push result
   */
  public GitCommitAndPushResult(GitCommitResult gitCommitResult, GitPushResult gitPushResult,
      YamlGitConfig yamlGitConfig, List<GitFileChange> filesCommitedToGit) {
    super(GitCommandType.COMMIT_AND_PUSH);
    this.gitCommitResult = gitCommitResult;
    this.gitPushResult = gitPushResult;
    this.yamlGitConfig = yamlGitConfig;
    this.filesCommitedToGit = filesCommitedToGit;
  }
}
