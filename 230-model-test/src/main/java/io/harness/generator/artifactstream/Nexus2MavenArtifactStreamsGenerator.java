/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;

import static java.util.Arrays.asList;

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

import com.google.inject.Singleton;

@OwnedBy(CDP)
@Singleton
public class Nexus2MavenArtifactStreamsGenerator extends NexusArtifactStreamsGenerator {
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
        settingGenerator.ensurePredefined(seed, owners, SettingGenerator.Settings.HARNESS_NEXUS2_CONNECTOR);
    return ensureArtifactStream(seed,
        NexusArtifactStream.builder()
            .appId(atConnector ? GLOBAL_APP_ID : application.getUuid())
            .serviceId(atConnector ? settingAttribute.getUuid() : getServiceId(service))
            .autoPopulate(false)
            .metadataOnly(metadataOnly)
            .name(metadataOnly ? "nexus2-maven-todolist-metadataOnly" : "nexus2-maven-todolist")
            .sourceName(settingAttribute.getName())
            .repositoryFormat(RepositoryFormat.maven.name())
            .jobname("releases")
            .groupId("mygroup")
            .artifactPaths(asList("todolist"))
            .settingId(settingAttribute.getUuid())
            .build(),
        owners);
  }
}
