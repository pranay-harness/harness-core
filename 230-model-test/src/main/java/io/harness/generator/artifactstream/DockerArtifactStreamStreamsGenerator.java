/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;

import io.harness.annotations.dev.OwnedBy;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;

import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(CDP)
@Singleton
public class DockerArtifactStreamStreamsGenerator implements ArtifactStreamsGenerator {
  @Inject private SettingGenerator settingGenerator;
  @Inject private ArtifactStreamGeneratorHelper artifactStreamGeneratorHelper;

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners) {
    return ensureArtifactStream(seed, owners, false);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, String serviceName, boolean atConnector) {
    Service service = owners.obtainServiceByServiceName(serviceName);
    Application application = owners.obtainApplication();
    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.HARNESS_DOCKER_REGISTRY);

    ArtifactStream artifactStream = DockerArtifactStream.builder()
                                        .appId(atConnector ? GLOBAL_APP_ID : application.getUuid())
                                        .name(atConnector ? "nginx-atConnector" : "nginx")
                                        .serviceId(atConnector    ? settingAttribute.getUuid()
                                                : service != null ? service.getUuid()
                                                                  : null)
                                        .settingId(settingAttribute.getUuid())
                                        .imageName("library/nginx")
                                        .build();
    return ensureArtifactStream(seed, artifactStream, owners);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector) {
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();
    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.HARNESS_DOCKER_REGISTRY);

    ArtifactStream artifactStream = DockerArtifactStream.builder()
                                        .appId(atConnector ? GLOBAL_APP_ID : application.getUuid())
                                        .name(atConnector ? "nginx-atConnector" : "nginx")
                                        .serviceId(atConnector    ? settingAttribute.getUuid()
                                                : service != null ? service.getUuid()
                                                                  : null)
                                        .settingId(settingAttribute.getUuid())
                                        .imageName("library/nginx")
                                        .build();
    return ensureArtifactStream(seed, artifactStream, owners);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector, boolean metadataOnly) {
    return null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, ArtifactStream artifactStream, Owners owners) {
    DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;
    ArtifactStream existing = artifactStreamGeneratorHelper.exists(dockerArtifactStream);
    if (existing != null) {
      return existing;
    }
    return artifactStreamGeneratorHelper.saveArtifactStream(DockerArtifactStream.builder()
                                                                .appId(dockerArtifactStream.getAppId())
                                                                .serviceId(dockerArtifactStream.getServiceId())
                                                                .name(dockerArtifactStream.getName())
                                                                .imageName(dockerArtifactStream.getImageName())
                                                                .autoPopulate(dockerArtifactStream.isAutoPopulate())
                                                                .settingId(dockerArtifactStream.getSettingId())
                                                                .build(),
        owners);
  }
}
