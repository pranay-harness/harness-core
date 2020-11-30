package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("dockerhubConnector")
public class DockerhubConnector implements ArtifactConnector {
  @NotNull private String connectorRef;
  @Builder.Default @NotNull Type type = Type.DOCKERHUB;
}
