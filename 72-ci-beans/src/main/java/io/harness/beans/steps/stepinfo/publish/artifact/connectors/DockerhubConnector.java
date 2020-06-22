package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class DockerhubConnector implements ArtifactConnector {
  @NotNull private String connector;
  @Override
  public Type getType() {
    return Type.DOCKERHUB;
  }
}
