package io.harness.cdng.artifact.delegate;

import io.harness.cdng.artifact.delegate.beans.ArtifactAttributes;
import io.harness.cdng.artifact.delegate.beans.ArtifactSourceAttributes;
import io.harness.cdng.artifact.delegate.beans.connector.ConnectorConfig;

public interface DelegateArtifactService {
  default ArtifactAttributes getLastSuccessfulBuild(
      String appId, ArtifactSourceAttributes sourceAttributes, ConnectorConfig config) {
    throw new UnsupportedOperationException();
  }
}
