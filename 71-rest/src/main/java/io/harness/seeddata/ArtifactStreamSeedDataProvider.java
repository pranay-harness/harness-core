package io.harness.seeddata;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.service.intfc.ArtifactStreamService;

@Singleton
public class ArtifactStreamSeedDataProvider {
  @Inject public ArtifactStreamService artifactStreamService;

  public ArtifactStream createDockerArtifactStream(String appId, String serviceId, String settingId) {
    return artifactStreamService.forceCreate(DockerArtifactStream.builder()
                                                 .appId(appId)
                                                 .settingId(settingId)
                                                 .imageName(SeedDataProviderConstants.DOCKER_TODO_LIST_IMAGE_NAME)
                                                 .name(SeedDataProviderConstants.DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME)
                                                 .serviceId(serviceId)
                                                 .build());
  }
}
