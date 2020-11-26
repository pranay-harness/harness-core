package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("dockerhubConnector")
public class DockerhubConnector implements ArtifactConnector {
  @NotNull private String connectorRef;
  @Override
  public Type getType() {
    return Type.DOCKERHUB;
  }
}
