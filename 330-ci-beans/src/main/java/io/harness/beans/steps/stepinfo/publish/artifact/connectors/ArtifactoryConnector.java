package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("artifactoryConnector")
public class ArtifactoryConnector implements ArtifactConnector {
  @NotNull private String connectorRef;
  @NotNull private String repository;
  @NotNull private String artifactPath;
  @Builder.Default @NotNull Type type = Type.ARTIFACTORY;
}
