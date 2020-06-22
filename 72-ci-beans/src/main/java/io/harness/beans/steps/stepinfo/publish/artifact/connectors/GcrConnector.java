package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class GcrConnector implements ArtifactConnector {
  @NotNull private String connector;
  @NotNull private String location;
  @Override
  public Type getType() {
    return Type.GCR;
  }
}
