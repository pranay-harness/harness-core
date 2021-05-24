package io.harness.version;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PL)
public class VersionInfo {
  private String version;
  private String buildNo;
  private String gitCommit;
  private String gitBranch;
  private String timestamp;
  private String fullVersionWithPatch;
}
