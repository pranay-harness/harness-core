package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("artifactoryConnector")
public class ArtifactoryConnector implements ArtifactConnector {
  @NotNull private String connectorRef;
  @NotNull private String repository;
  @NotNull private String artifactPath;
  @Override
  public Type getType() {
    return Type.ARTIFACTORY;
  }
}
