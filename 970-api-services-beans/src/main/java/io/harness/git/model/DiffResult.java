/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
