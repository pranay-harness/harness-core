package io.harness.ngpipeline.status;

import io.harness.annotation.RecasterAlias;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("buildStatusUpdateParameter")
@RecasterAlias("io.harness.ngpipeline.status.BuildStatusUpdateParameter")
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
  private String repoName;
}
