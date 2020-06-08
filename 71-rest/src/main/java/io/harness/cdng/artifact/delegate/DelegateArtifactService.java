package io.harness.cdng.artifact.delegate;

import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.ArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.ConnectorConfig;

public interface DelegateArtifactService<T extends ConnectorConfig> {
  default ArtifactAttributes getLastSuccessfulBuild(String appId, ArtifactSourceAttributes sourceAttributes, T config) {
    throw new UnsupportedOperationException();
  }
}
