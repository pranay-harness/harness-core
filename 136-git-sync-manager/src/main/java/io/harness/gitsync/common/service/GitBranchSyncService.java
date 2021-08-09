package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.common.dtos.GitToHarnessProcessMsvcStepResponse;

@OwnedBy(DX)
public interface GitBranchSyncService {
  GitToHarnessProcessMsvcStepResponse processBranchSyncEvent(YamlGitConfigDTO yamlGitConfigDTO, String branch,
      String accountIdentifier, String filePathToBeExcluded, String changeSetId, String gitToHarnessProgressRecordId);

  void createBranchSyncEvent(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String yamlGitConfigIdentifier, String repoURL, String branch, String filePathToBeExcluded);
}
