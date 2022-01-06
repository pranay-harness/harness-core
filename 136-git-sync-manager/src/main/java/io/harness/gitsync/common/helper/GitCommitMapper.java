/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.dtos.GitCommitDTO;

import lombok.experimental.UtilityClass;

@OwnedBy(DX)
@UtilityClass
public class GitCommitMapper {
  public GitCommitDTO toGitCommitDTO(GitCommit gitCommit) {
    return GitCommitDTO.builder()
        .accountIdentifier(gitCommit.getAccountIdentifier())
        .branchName(gitCommit.getBranchName())
        .commitId(gitCommit.getCommitId())
        .commitMessage(gitCommit.getCommitMessage())
        .failureReason(gitCommit.getFailureReason())
        .fileProcessingSummary(gitCommit.getFileProcessingSummary())
        .gitSyncDirection(gitCommit.getGitSyncDirection())
        .repoURL(gitCommit.getRepoURL())
        .status(gitCommit.getStatus())
        .lastUpdatedAt(gitCommit.getLastUpdatedAt())
        .build();
  }
}
