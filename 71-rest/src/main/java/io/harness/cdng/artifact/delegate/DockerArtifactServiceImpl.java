package io.harness.cdng.artifact.delegate;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.delegate.resource.DockerRegistryService;

@Singleton
public class DockerArtifactServiceImpl implements DockerArtifactService {
  @Inject private DockerRegistryService dockerRegistryService;

  @Override
  public ArtifactAttributes getLastSuccessfulBuild(
      String appId, ArtifactSourceAttributes streamAttributes, DockerhubConnectorConfig connectorConfig) {
    DockerArtifactSourceAttributes dockerArtifactStreamAttributes = (DockerArtifactSourceAttributes) streamAttributes;
    return dockerRegistryService.getLastSuccessfulBuild(
        connectorConfig, dockerArtifactStreamAttributes.getImagePath(), dockerArtifactStreamAttributes.getTag());
  }
}
