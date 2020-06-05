package io.harness.cdng.artifact.delegate;

import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;

public interface DockerArtifactService extends DelegateArtifactService<DockerhubConnectorConfig> {
  @Override
  ArtifactAttributes getLastSuccessfulBuild(
      String appId, ArtifactSourceAttributes streamAttributes, DockerhubConnectorConfig config);
}
