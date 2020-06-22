package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class NexusConnector implements ArtifactConnector {
  @NotNull private String connector;
  @NotNull private String path;
  @Override
  public Type getType() {
    return Type.NEXUS;
  }
}
