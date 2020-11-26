package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("gcrConnector")
public class GcrConnector implements ArtifactConnector {
  @NotNull private String connectorRef;
  @NotNull private String location;
  @Override
  public Type getType() {
    return Type.GCR;
  }
}
