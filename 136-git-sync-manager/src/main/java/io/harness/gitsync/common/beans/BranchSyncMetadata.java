package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class BranchSyncMetadata implements EventMetadata {
  String projectIdentifier;
  String orgIdentifier;
  String yamlGitConfigId;
  String fileToBeExcluded;
}
