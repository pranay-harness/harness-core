package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtifactoryConnector implements ArtifactConnector {
  @NotNull private String connectorRef;
  @NotNull private String repository;
  @NotNull private String artifactPath;
  @Override
  public Type getType() {
    return Type.ARTIFACTORY;
  }
}
