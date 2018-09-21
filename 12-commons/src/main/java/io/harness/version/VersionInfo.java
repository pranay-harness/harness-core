package io.harness.version;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VersionInfo {
  private String version;
  private String buildNo;
  private String gitCommit;
  private String gitBranch;
  private String timestamp;
}
