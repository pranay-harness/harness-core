package io.harness.gitsync.gitsyncerror.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;

import java.util.List;

@OwnedBy(DX)
public interface GitSyncErrorService {
  void upsertGitSyncErrors(
      GitFileChange failedChange, String errorMessage, boolean fullSyncPath, YamlGitConfigDTO yamlGitConfig);

  List<GitSyncError> getActiveGitToHarnessSyncErrors(String accountId, String gitConnectorId, String repoName,
      String branchName, String rootFolder, long fromTimestamp);

  boolean deleteGitSyncErrors(List<String> errorIds, String accountId);
}
