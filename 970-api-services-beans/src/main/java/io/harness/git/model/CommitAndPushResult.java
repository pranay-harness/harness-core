/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.git.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CommitAndPushResult extends GitBaseResult {
  private CommitResult gitCommitResult;
  private PushResultGit gitPushResult;
  private List<GitFileChange> filesCommittedToGit;

  @Builder
  public CommitAndPushResult(String accountId, CommitResult gitCommitResult, PushResultGit gitPushResult,
      List<GitFileChange> filesCommittedToGit) {
    super(accountId);
    this.gitCommitResult = gitCommitResult;
    this.gitPushResult = gitPushResult;
    this.filesCommittedToGit = filesCommittedToGit;
  }
}
