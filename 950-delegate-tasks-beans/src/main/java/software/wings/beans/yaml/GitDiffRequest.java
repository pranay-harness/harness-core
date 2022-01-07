/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.yaml;

import software.wings.yaml.gitSync.YamlGitConfig;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
