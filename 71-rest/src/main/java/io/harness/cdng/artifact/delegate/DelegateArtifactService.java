package io.harness.cdng.artifact.delegate;

import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.ConnectorConfig;

public interface DelegateArtifactService<T extends ConnectorConfig> {
  default ArtifactAttributes getLastSuccessfulBuild(String appId, ArtifactSourceAttributes streamAttributes, T config) {
    throw new UnsupportedOperationException();
  }
}
