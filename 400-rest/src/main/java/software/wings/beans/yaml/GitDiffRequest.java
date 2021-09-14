/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
