/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitsyncerror.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static io.fabric8.utils.Strings.nullIfEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorAggregateByCommit;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorDetails;
import io.harness.gitsync.gitsyncerror.beans.GitToHarnessErrorDetails;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorAggregateByCommitDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorDetailsDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitToHarnessErrorDetailsDTO;

import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PL)
public class GitSyncErrorMapper {
  public GitSyncErrorDTO toGitSyncErrorDTO(GitSyncError gitSyncError) {
    Optional<GitSyncErrorDetailsDTO> additionalErrorDetails =
        toGitSyncErrorDetailsDTO(gitSyncError.getAdditionalErrorDetails());
    return GitSyncErrorDTO.builder()
        .accountIdentifier(gitSyncError.getAccountIdentifier())
        .repoUrl(nullIfEmpty(gitSyncError.getRepoUrl()))
        .branchName(nullIfEmpty(gitSyncError.getBranchName()))
        .scopes(gitSyncError.getScopes())
        .completeFilePath(nullIfEmpty(gitSyncError.getCompleteFilePath()))
        .errorType(gitSyncError.getErrorType())
        .changeType(gitSyncError.getChangeType())
        .entityType(gitSyncError.getEntityType())
        .status(gitSyncError.getStatus())
        .failureReason(gitSyncError.getFailureReason())
        .additionalErrorDetails(additionalErrorDetails.isPresent() ? additionalErrorDetails.get() : null)
        .createdAt(gitSyncError.getCreatedAt())
        .build();
  }

  public GitSyncError toGitSyncError(GitSyncErrorDTO gitSyncErrorDTO, String accountIdentifier) {
    Optional<GitSyncErrorDetails> additionalErrorDetails =
        toGitSyncErrorDetails(gitSyncErrorDTO.getAdditionalErrorDetails());
    return GitSyncError.builder()
        .accountIdentifier(accountIdentifier)
        .repoUrl(nullIfEmpty(gitSyncErrorDTO.getRepoUrl()))
        .branchName(nullIfEmpty(gitSyncErrorDTO.getBranchName()))
        .scopes(gitSyncErrorDTO.getScopes())
        .completeFilePath(nullIfEmpty(gitSyncErrorDTO.getCompleteFilePath()))
        .errorType(gitSyncErrorDTO.getErrorType())
        .changeType(gitSyncErrorDTO.getChangeType())
        .entityType(gitSyncErrorDTO.getEntityType())
        .status(gitSyncErrorDTO.getStatus())
        .failureReason(gitSyncErrorDTO.getFailureReason())
        .additionalErrorDetails(additionalErrorDetails.isPresent() ? additionalErrorDetails.get() : null)
        .build();
  }

  public GitSyncErrorAggregateByCommitDTO toGitSyncErrorAggregateByCommitDTO(
      GitSyncErrorAggregateByCommit gitSyncErrorAggregateByCommit) {
    return GitSyncErrorAggregateByCommitDTO.builder()
        .gitCommitId(gitSyncErrorAggregateByCommit.getGitCommitId())
        .failedCount(gitSyncErrorAggregateByCommit.getFailedCount())
        .repoId(gitSyncErrorAggregateByCommit.getRepoId())
        .branchName(gitSyncErrorAggregateByCommit.getBranchName())
        .commitMessage(gitSyncErrorAggregateByCommit.getCommitMessage())
        .createdAt(gitSyncErrorAggregateByCommit.getCreatedAt())
        .errorsForSummaryView(gitSyncErrorAggregateByCommit.getErrorsForSummaryView()
                                  .stream()
                                  .map(GitSyncErrorMapper::toGitSyncErrorDTO)
                                  .collect(Collectors.toList()))
        .build();
  }

  private Optional<GitSyncErrorDetails> toGitSyncErrorDetails(GitSyncErrorDetailsDTO gitSyncErrorDetailsDTO) {
    if (gitSyncErrorDetailsDTO != null) {
      GitToHarnessErrorDetailsDTO gitToHarnessErrorDetailsDTO = (GitToHarnessErrorDetailsDTO) gitSyncErrorDetailsDTO;
      return Optional.of(GitToHarnessErrorDetails.builder()
                             .commitMessage(gitToHarnessErrorDetailsDTO.getCommitMessage())
                             .gitCommitId(gitToHarnessErrorDetailsDTO.getGitCommitId())
                             .yamlContent(gitToHarnessErrorDetailsDTO.getYamlContent())
                             .resolvedByCommitId(gitToHarnessErrorDetailsDTO.getResolvedByCommitId())
                             .build());
    }
    return Optional.empty();
  }

  private Optional<GitSyncErrorDetailsDTO> toGitSyncErrorDetailsDTO(GitSyncErrorDetails gitSyncErrorDetails) {
    if (gitSyncErrorDetails != null) {
      GitToHarnessErrorDetails gitToHarnessErrorDetails = (GitToHarnessErrorDetails) gitSyncErrorDetails;
      return Optional.of(GitToHarnessErrorDetailsDTO.builder()
                             .commitMessage(gitToHarnessErrorDetails.getCommitMessage())
                             .gitCommitId(gitToHarnessErrorDetails.getGitCommitId())
                             .yamlContent(gitToHarnessErrorDetails.getYamlContent())
                             .resolvedByCommitId(gitToHarnessErrorDetails.getResolvedByCommitId())
                             .build());
    }
    return Optional.empty();
  }
}
