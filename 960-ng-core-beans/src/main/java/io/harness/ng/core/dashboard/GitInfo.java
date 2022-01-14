package io.harness.ng.core.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class GitInfo {
  private String sourceBranch;
  private String targetBranch;
  private String commit;
  private String commitID;
  private String eventType;
  private String repoName;
}
