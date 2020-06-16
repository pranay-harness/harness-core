package io.harness.cdng.artifact.delegate.resource;

import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;

public interface DockerRegistryService {
  int MAX_NO_OF_TAGS_PER_IMAGE = 10000;
  int MAX_NO_OF_TAGS_PER_PUBLIC_IMAGE = 250;

  /**
   * Gets last successful image tag from a given tag.
   *
   * @param connectorConfig
   * @param imageName
   * @param tag
   * @return
   */
  ArtifactAttributes getLastSuccessfulBuild(DockerhubConnectorConfig connectorConfig, String imageName, String tag);

  /**
   * Gets last successful image tag from a tagRegex.
   * @param connectorConfig
   * @param imageName
   * @param tagRegex
   * @return
   */
  ArtifactAttributes getLastSuccessfulBuildFromRegex(
      DockerhubConnectorConfig connectorConfig, String imageName, String tagRegex);
}
