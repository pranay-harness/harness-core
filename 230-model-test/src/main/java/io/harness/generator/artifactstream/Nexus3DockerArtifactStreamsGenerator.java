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
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.SettingGenerator;

import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.utils.RepositoryFormat;

@OwnedBy(CDP)
public class Nexus3DockerArtifactStreamsGenerator extends NexusArtifactStreamsGenerator {
  @Override
  public ArtifactStream ensureArtifactStream(
      Randomizer.Seed seed, OwnerManager.Owners owners, String serviceName, boolean atConnector) {
    return null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(
      Randomizer.Seed seed, OwnerManager.Owners owners, boolean atConnector, boolean metadataOnly) {
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();
    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, SettingGenerator.Settings.HARNESS_NEXU3_CONNECTOR);
    return ensureArtifactStream(seed,
        NexusArtifactStream.builder()
            .appId(atConnector ? GLOBAL_APP_ID : application.getUuid())
            .serviceId(atConnector ? settingAttribute.getUuid() : getServiceId(service))
            .autoPopulate(false)
            .metadataOnly(metadataOnly)
            .name(metadataOnly ? "nexus3-docker-metadataOnly" : "nexus3-docker")
            .sourceName(settingAttribute.getName())
            .repositoryFormat(RepositoryFormat.docker.name())
            .jobname("todolist")
            .imageName("todolist")
            .dockerRegistryUrl("nexus3.dev.harness.io:8082")
            .settingId(settingAttribute.getUuid())
            .build(),
        owners);
  }
}
