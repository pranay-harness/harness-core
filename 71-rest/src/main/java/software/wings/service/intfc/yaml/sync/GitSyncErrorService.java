package software.wings.service.intfc.yaml.sync;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.yaml.Change;
import software.wings.service.impl.yaml.GitToHarnessErrorCommitStats;
import software.wings.yaml.errorhandling.GitProcessingError;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitFileActivity;

import java.util.List;

public interface GitSyncErrorService {
  PageResponse<GitToHarnessErrorCommitStats> listGitToHarnessErrorsCommits(
      PageRequest<GitToHarnessErrorCommitStats> req, String accountId, String gitConnectorId, String branchName);

  PageResponse<GitSyncError> listAllGitToHarnessErrors(
      PageRequest<GitSyncError> req, String accountId, String gitConnectorId, String branchName);

  PageResponse<GitSyncError> fetchErrorsInEachCommits(PageRequest<GitSyncError> req, String gitCommitId,
      String accountId, List<String> includeDataList, String yamlFilePath);

  <T extends Change> void upsertGitSyncErrors(
      T failedChange, String errorMessage, boolean fullSyncPath, boolean gitToHarness);

  List<GitSyncError> getActiveGitToHarnessSyncErrors(
      String accountId, String gitConnectorId, String branchName, long fromTimestamp);

  PageResponse<GitSyncError> fetchHarnessToGitErrors(
      PageRequest<GitSyncError> req, String accountId, String gitConnectorId, String branchName);

  PageResponse<GitSyncError> fetchErrors(PageRequest<GitSyncError> req);

  void deleteGitSyncErrorAndLogFileActivity(List<String> errorIds, GitFileActivity.Status status, String accountId);

  long getGitSyncErrorCount(String accountId);

  PageResponse<GitProcessingError> fetchGitConnectivityIssues(PageRequest<GitProcessingError> req, String accountId);

  long getTotalGitErrorsCount(String accountId);
}
