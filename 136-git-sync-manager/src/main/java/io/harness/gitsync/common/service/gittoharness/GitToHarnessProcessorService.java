package io.harness.gitsync.common.service.gittoharness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProcessingResponse;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public interface GitToHarnessProcessorService {
  List<GitToHarnessProcessingResponse> processFiles(String accountId,
      List<GitToHarnessFileProcessingRequest> filesToBeProcessed, String branch, YamlGitConfigDTO yamlGitConfigDTO,
      String gitToHarnessProgressRecordId);
}
