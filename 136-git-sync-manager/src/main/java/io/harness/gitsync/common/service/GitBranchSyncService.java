package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.common.beans.YamlChangeSet;

@OwnedBy(DX)
public interface GitBranchSyncService {
  void syncBranch(YamlGitConfigDTO yamlGitConfigDTO, String branch, String accountId, String filePathToBeExcluded,
      YamlChangeSet yamlChangeSet);
}
