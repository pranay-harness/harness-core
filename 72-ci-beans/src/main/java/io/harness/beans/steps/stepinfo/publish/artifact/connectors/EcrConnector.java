package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class EcrConnector implements ArtifactConnector {
  @NotNull private String connector;
  @NotNull private String location;
  @NotNull private String region;
  @Override
  public Type getType() {
    return Type.ECR;
  }
}
