package io.harness.gitsync.gitsyncerror.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static io.fabric8.utils.Strings.nullIfEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorAggregateByCommit;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorDetails;
import io.harness.gitsync.gitsyncerror.beans.GitToHarnessErrorDetails;
import io.harness.gitsync.gitsyncerror.beans.HarnessToGitErrorDetails;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorAggregateByCommitDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorDetailsDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitToHarnessErrorDetailsDTO;
import io.harness.gitsync.gitsyncerror.dtos.HarnessToGitErrorDetailsDTO;

import java.util.stream.Collectors;
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
        .additionalErrorDetails(toGitSyncErrorDetailsDTO(gitSyncError.getAdditionalErrorDetails()))
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
        .additionalErrorDetails(toGitSyncErrorDetails(gitSyncErrorDTO.getAdditionalErrorDetails()))
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
        .errorsForSummaryView(gitSyncErrorAggregateByCommit.getErrorsForSummaryView()
                                  .stream()
                                  .map(GitSyncErrorMapper::toGitSyncErrorDTO)
                                  .collect(Collectors.toList()))
        .build();
  }

  private GitSyncErrorDetails toGitSyncErrorDetails(GitSyncErrorDetailsDTO gitSyncErrorDetailsDTO) {
    if (gitSyncErrorDetailsDTO instanceof GitToHarnessErrorDetailsDTO) {
      GitToHarnessErrorDetailsDTO gitToHarnessErrorDetailsDTO = (GitToHarnessErrorDetailsDTO) gitSyncErrorDetailsDTO;
      return GitToHarnessErrorDetails.builder()
          .commitMessage(gitToHarnessErrorDetailsDTO.getCommitMessage())
          .gitCommitId(gitToHarnessErrorDetailsDTO.getGitCommitId())
          .yamlContent(gitToHarnessErrorDetailsDTO.getYamlContent())
          .resolvedByCommitId(gitToHarnessErrorDetailsDTO.getResolvedByCommitId())
          .build();
    } else {
      HarnessToGitErrorDetailsDTO harnessToGitErrorDetailsDTO = (HarnessToGitErrorDetailsDTO) gitSyncErrorDetailsDTO;
      return HarnessToGitErrorDetails.builder()
          .orgIdentifier(harnessToGitErrorDetailsDTO.getOrgIdentifier())
          .projectIdentifier(harnessToGitErrorDetailsDTO.getProjectIdentifier())
          .build();
    }
  }

  private GitSyncErrorDetailsDTO toGitSyncErrorDetailsDTO(GitSyncErrorDetails gitSyncErrorDetails) {
    if (gitSyncErrorDetails instanceof GitToHarnessErrorDetails) {
      GitToHarnessErrorDetails gitToHarnessErrorDetails = (GitToHarnessErrorDetails) gitSyncErrorDetails;
      return GitToHarnessErrorDetailsDTO.builder()
          .commitMessage(gitToHarnessErrorDetails.getCommitMessage())
          .gitCommitId(gitToHarnessErrorDetails.getGitCommitId())
          .yamlContent(gitToHarnessErrorDetails.getYamlContent())
          .resolvedByCommitId(gitToHarnessErrorDetails.getResolvedByCommitId())
          .build();
    } else {
      HarnessToGitErrorDetails harnessToGitErrorDetails = (HarnessToGitErrorDetails) gitSyncErrorDetails;
      return HarnessToGitErrorDetailsDTO.builder()
          .orgIdentifier(harnessToGitErrorDetails.getOrgIdentifier())
          .projectIdentifier(harnessToGitErrorDetails.getProjectIdentifier())
          .build();
    }
  }
}
