package io.harness.gitsync.common.service.gittoharness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;

@OwnedBy(HarnessTeam.DX)
public interface GitToHarnessProcessorService {
  void readFilesFromBranchAndProcess(YamlGitConfigDTO yamlGitConfigDTO, String branch, String accountId);
}
