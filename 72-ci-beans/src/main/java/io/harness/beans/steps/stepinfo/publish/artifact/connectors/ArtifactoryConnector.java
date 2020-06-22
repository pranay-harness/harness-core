package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class ArtifactoryConnector implements ArtifactConnector {
  @NotNull private String connector;
  @NotNull private String repository;
  @NotNull private String artifactPath;
  @Override
  public Type getType() {
    return Type.ARTIFACTORY;
  }
}
