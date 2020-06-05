package io.harness.cdng.artifact.delegate.resource;

import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;

public interface DockerRegistryService {
  ArtifactAttributes getLastSuccessfulBuild(DockerhubConnectorConfig connectorConfig, String imageName, String tag);
}
