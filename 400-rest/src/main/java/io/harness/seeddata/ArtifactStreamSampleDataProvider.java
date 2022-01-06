/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.DOCKER_TODO_LIST_IMAGE_NAME;

import software.wings.beans.DockerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ArtifactStreamSampleDataProvider {
  @Inject public ArtifactStreamService artifactStreamService;
  @Inject public BuildSourceService buildSourceService;

  public ArtifactStream createDockerArtifactStream(String appId, String serviceId, SettingAttribute settingAttribute) {
    ArtifactStream savedArtifactStream = artifactStreamService.createWithBinding(appId,
        DockerArtifactStream.builder()
            .appId(appId)
            .settingId(settingAttribute.getUuid())
            .imageName(DOCKER_TODO_LIST_IMAGE_NAME)
            .name(SampleDataProviderConstants.DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME)
            .serviceId(serviceId)
            .sample(true)
            .build(),
        false);

    try {
      addTag(appId, savedArtifactStream, (DockerConfig) settingAttribute.getValue());
    } catch (Exception e) {
      log.warn("Error occurred while saving artifacts for docker ArtifactStream for accountId {} ",
          settingAttribute.getAccountId());
    }
    return savedArtifactStream;
  }

  private void addTag(String appId, ArtifactStream savedArtifactStream, DockerConfig dockerConfig) {
    addTag(appId, savedArtifactStream, dockerConfig, "1");

    // Add second tag
    addTag(appId, savedArtifactStream, dockerConfig, "2");

    addTag(appId, savedArtifactStream, dockerConfig, "3");

    addTag(appId, savedArtifactStream, dockerConfig, "4");

    addTag(appId, savedArtifactStream, dockerConfig, "5");

    addTag(appId, savedArtifactStream, dockerConfig, "6");

    addTag(appId, savedArtifactStream, dockerConfig, "7");

    addTag(appId, savedArtifactStream, dockerConfig, "8");

    addTag(appId, savedArtifactStream, dockerConfig, "9");

    addTag(appId, savedArtifactStream, dockerConfig, "latest");
  }

  private void addTag(String appId, ArtifactStream savedArtifactStream, DockerConfig dockerConfig, String s) {
    BuildDetails buildDetails =
        ArtifactCollectionUtils.prepareDockerBuildDetails(dockerConfig, DOCKER_TODO_LIST_IMAGE_NAME, s);
    buildSourceService.collectArtifact(appId, savedArtifactStream.getUuid(), buildDetails);
  }
}
