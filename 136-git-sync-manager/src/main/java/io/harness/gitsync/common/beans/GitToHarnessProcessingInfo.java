package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(PL)
public class GitToHarnessProcessingInfo {
  String accountId;
  String repoUrl;
  String branchName;
  String commitId;
  String gitToHarnessProgressRecordId;
  String commitMessage;
  List<YamlGitConfigDTO> yamlGitConfigs;
}
