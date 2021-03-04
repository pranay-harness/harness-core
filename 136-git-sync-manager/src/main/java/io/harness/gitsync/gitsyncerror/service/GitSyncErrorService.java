package io.harness.gitsync.gitsyncerror.service;

import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;

import java.util.List;

public interface GitSyncErrorService {
  void upsertGitSyncErrors(
      GitFileChange failedChange, String errorMessage, boolean fullSyncPath, YamlGitConfigDTO yamlGitConfig);

  List<GitSyncError> getActiveGitToHarnessSyncErrors(String accountId, String gitConnectorId, String repoName,
      String branchName, String rootFolder, long fromTimestamp);

  boolean deleteGitSyncErrors(List<String> errorIds, String accountId);

  void deleteByAccountIdOrgIdProjectIdAndFilePath(
      String accountId, String orgId, String projectId, List<String> yamlFilePath);
}
