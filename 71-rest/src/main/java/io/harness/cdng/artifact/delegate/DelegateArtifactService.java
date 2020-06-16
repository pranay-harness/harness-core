package io.harness.cdng.artifact.delegate;

import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.ArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.ConnectorConfig;

public interface DelegateArtifactService {
  default ArtifactAttributes getLastSuccessfulBuild(
      String appId, ArtifactSourceAttributes sourceAttributes, ConnectorConfig config) {
    throw new UnsupportedOperationException();
  }
}
