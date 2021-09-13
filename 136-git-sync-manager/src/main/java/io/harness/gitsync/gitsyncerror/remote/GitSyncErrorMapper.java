package io.harness.gitsync.gitsyncerror.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static io.fabric8.utils.Strings.nullIfEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PL)
public class GitSyncErrorMapper {
  public GitSyncErrorDTO toGitSyncErrorDTO(GitSyncError gitSyncError) {
    return GitSyncErrorDTO.builder()
        .accountIdentifier(gitSyncError.getAccountIdentifier())
        .repoUrl(nullIfEmpty(gitSyncError.getRepoUrl()))
        .branchName(nullIfEmpty(gitSyncError.getBranchName()))
        .completeFilePath(nullIfEmpty(gitSyncError.getCompleteFilePath()))
        .errorType(gitSyncError.getErrorType())
        .changeType(gitSyncError.getChangeType())
        .entityType(gitSyncError.getEntityType())
        .entityReference(gitSyncError.getEntityReference())
        .status(gitSyncError.getStatus())
        .failureReason(gitSyncError.getFailureReason())
        .additionalErrorDetails(gitSyncError.getAdditionalErrorDetails())
        .build();
  }

  public GitSyncError toGitSyncError(GitSyncErrorDTO gitSyncErrorDTO, String accountIdentifier) {
    return GitSyncError.builder()
        .accountIdentifier(accountIdentifier)
        .repoUrl(nullIfEmpty(gitSyncErrorDTO.getRepoUrl()))
        .branchName(nullIfEmpty(gitSyncErrorDTO.getBranchName()))
        .completeFilePath(nullIfEmpty(gitSyncErrorDTO.getCompleteFilePath()))
        .errorType(gitSyncErrorDTO.getErrorType())
        .changeType(gitSyncErrorDTO.getChangeType())
        .entityType(gitSyncErrorDTO.getEntityType())
        .entityReference(gitSyncErrorDTO.getEntityReference())
        .status(gitSyncErrorDTO.getStatus())
        .failureReason(gitSyncErrorDTO.getFailureReason())
        .additionalErrorDetails(gitSyncErrorDTO.getAdditionalErrorDetails())
        .build();
  }
}
