package io.harness.cdng.artifact.delegate;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.ArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.ConnectorConfig;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.delegate.resource.DockerRegistryService;
import io.harness.data.structure.EmptyPredicate;

@Singleton
public class DockerArtifactServiceImpl implements DelegateArtifactService {
  @Inject private DockerRegistryService dockerRegistryService;

  @VisibleForTesting
  boolean isRegex(DockerArtifactSourceAttributes dockerArtifactSourceAttributes) {
    return EmptyPredicate.isNotEmpty(dockerArtifactSourceAttributes.getTagRegex());
  }

  @Override
  public ArtifactAttributes getLastSuccessfulBuild(
      String appId, ArtifactSourceAttributes sourceAttributes, ConnectorConfig config) {
    DockerhubConnectorConfig connectorConfig = (DockerhubConnectorConfig) config;
    DockerArtifactSourceAttributes dockerArtifactSourceAttributes = (DockerArtifactSourceAttributes) sourceAttributes;
    if (isRegex(dockerArtifactSourceAttributes)) {
      return dockerRegistryService.getLastSuccessfulBuildFromRegex(
          connectorConfig, dockerArtifactSourceAttributes.getImagePath(), dockerArtifactSourceAttributes.getTagRegex());
    }
    return dockerRegistryService.getLastSuccessfulBuild(
        connectorConfig, dockerArtifactSourceAttributes.getImagePath(), dockerArtifactSourceAttributes.getTag());
  }
}
