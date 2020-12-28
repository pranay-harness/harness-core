package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import io.harness.pms.yaml.ParameterField;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("nexusConnector")
public class NexusConnector implements ArtifactConnector {
  @NotNull private ParameterField<String> connectorRef;
  @NotNull private ParameterField<String> path;
  @Builder.Default @NotNull Type type = Type.NEXUS;
}
