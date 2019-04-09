package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.DOCKER_TODO_LIST_IMAGE_NAME;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DockerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.artifact.ArtifactCollectionUtil;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;

@Singleton
public class ArtifactStreamSampleDataProvider {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactStreamSampleDataProvider.class);
  @Inject public ArtifactStreamService artifactStreamService;
  @Inject public BuildSourceService buildSourceService;

  public ArtifactStream createDockerArtifactStream(String appId, String serviceId, SettingAttribute settingAttribute) {
    ArtifactStream savedArtifactStream =
        artifactStreamService.create(DockerArtifactStream.builder()
                                         .appId(appId)
                                         .settingId(settingAttribute.getUuid())
                                         .imageName(DOCKER_TODO_LIST_IMAGE_NAME)
                                         .name(SampleDataProviderConstants.DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME)
                                         .serviceId(serviceId)
                                         .build(),
            false);

    try {
      // Adding some artifacts to get ready with deployment.
      DockerConfig dockerConfig = (DockerConfig) settingAttribute.getValue();

      // Add one tag
      BuildDetails buildDetails =
          ArtifactCollectionUtil.prepareDockerBuildDetails(dockerConfig, DOCKER_TODO_LIST_IMAGE_NAME, "1");
      buildSourceService.collectArtifact(appId, savedArtifactStream.getUuid(), buildDetails);

      // Add second tag
      buildDetails = ArtifactCollectionUtil.prepareDockerBuildDetails(dockerConfig, DOCKER_TODO_LIST_IMAGE_NAME, "2");
      buildSourceService.collectArtifact(appId, savedArtifactStream.getUuid(), buildDetails);

      buildDetails = ArtifactCollectionUtil.prepareDockerBuildDetails(dockerConfig, DOCKER_TODO_LIST_IMAGE_NAME, "3");
      buildSourceService.collectArtifact(appId, savedArtifactStream.getUuid(), buildDetails);

      buildDetails = ArtifactCollectionUtil.prepareDockerBuildDetails(dockerConfig, DOCKER_TODO_LIST_IMAGE_NAME, "4");
      buildSourceService.collectArtifact(appId, savedArtifactStream.getUuid(), buildDetails);

      buildDetails = ArtifactCollectionUtil.prepareDockerBuildDetails(dockerConfig, DOCKER_TODO_LIST_IMAGE_NAME, "5");
      buildSourceService.collectArtifact(appId, savedArtifactStream.getUuid(), buildDetails);

      buildDetails = ArtifactCollectionUtil.prepareDockerBuildDetails(dockerConfig, DOCKER_TODO_LIST_IMAGE_NAME, "6");
      buildSourceService.collectArtifact(appId, savedArtifactStream.getUuid(), buildDetails);

      buildDetails = ArtifactCollectionUtil.prepareDockerBuildDetails(dockerConfig, DOCKER_TODO_LIST_IMAGE_NAME, "7");
      buildSourceService.collectArtifact(appId, savedArtifactStream.getUuid(), buildDetails);

      buildDetails = ArtifactCollectionUtil.prepareDockerBuildDetails(dockerConfig, DOCKER_TODO_LIST_IMAGE_NAME, "8");
      buildSourceService.collectArtifact(appId, savedArtifactStream.getUuid(), buildDetails);

      buildDetails = ArtifactCollectionUtil.prepareDockerBuildDetails(dockerConfig, DOCKER_TODO_LIST_IMAGE_NAME, "9");
      buildSourceService.collectArtifact(appId, savedArtifactStream.getUuid(), buildDetails);

      buildDetails =
          ArtifactCollectionUtil.prepareDockerBuildDetails(dockerConfig, DOCKER_TODO_LIST_IMAGE_NAME, "latest");
      buildSourceService.collectArtifact(appId, savedArtifactStream.getUuid(), buildDetails);

    } catch (Exception e) {
      logger.warn("Error occurred while saving artifacts for docker ArtifactStream for accountId {} ",
          settingAttribute.getAccountId());
    }
    return savedArtifactStream;
  }
}
