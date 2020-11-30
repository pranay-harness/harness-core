package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("nexusConnector")
public class NexusConnector implements ArtifactConnector {
  @NotNull private String connectorRef;
  @NotNull private String path;
  @Builder.Default @NotNull Type type = Type.NEXUS;
}
