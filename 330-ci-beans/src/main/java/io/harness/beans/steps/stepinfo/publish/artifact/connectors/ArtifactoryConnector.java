package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import io.harness.pms.yaml.ParameterField;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("artifactoryConnector")
public class ArtifactoryConnector implements ArtifactConnector {
  @NotNull private ParameterField<String> connectorRef;
  @NotNull private ParameterField<String> repository;
  @NotNull private ParameterField<String> artifactPath;
  @Builder.Default @NotNull Type type = Type.ARTIFACTORY;
}
