package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class S3Connector implements ArtifactConnector {
  @NotNull private String connectorRef;
  @NotNull private String location;
  @Override
  public Type getType() {
    return Type.S3;
  }
}
