package io.harness.cdng.artifact.bean.artifactsource;

import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig.DockerSpec;
import io.harness.cdng.artifact.delegate.DelegateArtifactService;
import io.harness.cdng.artifact.delegate.DockerArtifactService;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * DTO object to be passed to delegate tasks.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class DockerArtifactSourceAttributes extends DockerSpec implements ArtifactSourceAttributes {
  @Builder(builderMethodName = "newBuilder")
  public DockerArtifactSourceAttributes(String dockerhubConnector, String imagePath, String tag, String tagRegex) {
    super(dockerhubConnector, imagePath, tag, tagRegex);
  }

  @Override
  public Class<? extends DelegateArtifactService<DockerhubConnectorConfig>> getDelegateArtifactService() {
    return DockerArtifactService.class;
  }
}
