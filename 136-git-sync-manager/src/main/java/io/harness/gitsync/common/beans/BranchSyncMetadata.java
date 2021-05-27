package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@Value
@OwnedBy(DX)
public class BranchSyncMetadata implements EventMetadata {
  String projectIdentifier;
  String orgIdentifier;
  String accountId;
  String yamlGitConfigId;
}
