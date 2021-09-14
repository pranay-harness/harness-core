/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.git.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DiffResult extends GitBaseResult {
  private String repoName;
  private String branch;
  private String commitId;
  private List<GitFileChange> gitFileChanges = new ArrayList<>();
  private Long commitTimeMs;
  private String commitMessage;

  @Builder
  public DiffResult(String accountId, String repoName, String branch, String commitId,
      List<GitFileChange> gitFileChanges, Long commitTimeMs, String commitMessage) {
    super(accountId);
    this.repoName = repoName;
    this.branch = branch;
    this.commitId = commitId;
    this.gitFileChanges = gitFileChanges;
    this.commitTimeMs = commitTimeMs;
    this.commitMessage = commitMessage;
  }

  public void addChangeFile(GitFileChange gitFileChange) {
    gitFileChanges.add(gitFileChange);
  }
}
