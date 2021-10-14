package io.harness.gitsync.gitsyncerror.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorAggregateByCommitDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorCountDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorDTO;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;

@OwnedBy(PL)
public interface GitSyncErrorService {
  PageResponse<GitSyncErrorAggregateByCommitDTO> listGitToHarnessErrorsGroupedByCommits(PageRequest pageRequest,
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String searchTerm, String repoId,
      String branch, Integer numberOfErrorsInSummary);

  PageResponse<GitSyncErrorDTO> listAllGitToHarnessErrors(PageRequest pageRequest, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String searchTerm, String repoId, String branch);

  PageResponse<GitSyncErrorDTO> listGitToHarnessErrorsForCommit(PageRequest pageRequest, String commitId,
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String repoId, String branch);

  GitSyncErrorDTO save(GitSyncErrorDTO gitSyncErrorDTO);

  void upsertGitSyncErrors(
      GitFileChange failedChange, String errorMessage, boolean fullSyncPath, YamlGitConfigDTO yamlGitConfig);

  List<GitSyncError> getActiveGitToHarnessSyncErrors(String accountId, String gitConnectorId, String repoName,
      String branchName, String rootFolder, long fromTimestamp);

  boolean deleteGitSyncErrors(List<String> errorIds, String accountId);

  void recordConnectivityError(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      GitSyncErrorType gitSyncErrorType, String repoUrl, String branch, String errorMessage);

  PageResponse<GitSyncErrorDTO> listConnectivityErrors(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String repoIdentifier, String branch, PageRequest pageRequest);

  GitSyncErrorCountDTO getErrorCount(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String searchTerm, String repoId, String branch);
}
