package io.harness.seeddata;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.service.intfc.ArtifactStreamService;

@Singleton
public class ArtifactStreamSampleDataProvider {
  @Inject public ArtifactStreamService artifactStreamService;

  public ArtifactStream createDockerArtifactStream(String appId, String serviceId, String settingId) {
    return artifactStreamService.create(DockerArtifactStream.builder()
                                            .appId(appId)
                                            .settingId(settingId)
                                            .imageName(SampleDataProviderConstants.DOCKER_TODO_LIST_IMAGE_NAME)
                                            .name(SampleDataProviderConstants.DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME)
                                            .serviceId(serviceId)
                                            .build(),
        false);
  }
}
