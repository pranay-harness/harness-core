package io.harness.ngpipeline.status;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BuildStatusUpdateParameter implements BuildUpdateParameters {
  @Override
  public BuildUpdateType getBuildUpdateType() {
    return BuildUpdateType.STATUS;
  }
  private String label;
  private String title;
  private String desc;
  private String state;
  private String buildNumber;
  private String sha;
  private String identifier;
  private String connectorIdentifier;
  // Harsh TODO Also add github app or connector details. It is not finalised yet.
}
