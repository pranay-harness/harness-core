package io.harness.cdng.artifact.bean.artifactsource;

import io.harness.cdng.artifact.bean.connector.ConnectorConfig;
import io.harness.cdng.artifact.delegate.DelegateArtifactService;

/**
 * Interface of DTO to be passed to Delegate Tasks.
 */
public interface ArtifactSourceAttributes {
  Class<? extends DelegateArtifactService<? extends ConnectorConfig>> getDelegateArtifactService();
}
