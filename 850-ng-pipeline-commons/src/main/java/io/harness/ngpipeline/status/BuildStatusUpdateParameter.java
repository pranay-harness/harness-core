package io.harness.ngpipeline.status;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("buildStatusUpdateParameter")
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
  private String name;
  private String connectorIdentifier;
  // Harsh TODO Also add github app or connector details. It is not finalised yet.
}
