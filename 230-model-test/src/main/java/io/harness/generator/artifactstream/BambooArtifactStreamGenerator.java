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
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream.BambooArtifactStreamBuilder;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(CDP)
@Singleton
public class BambooArtifactStreamGenerator implements ArtifactStreamsGenerator {
  @Inject private ArtifactStreamGeneratorHelper artifactStreamGeneratorHelper;
  @Inject private SettingGenerator settingGenerator;

  @Override
  public ArtifactStream ensureArtifactStream(Randomizer.Seed seed, OwnerManager.Owners owners) {
    return ensureArtifactStream(seed, owners, false);
  }

  @Override
  public ArtifactStream ensureArtifactStream(
      Randomizer.Seed seed, OwnerManager.Owners owners, String serviceName, boolean atConnector) {
    return null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(Randomizer.Seed seed, OwnerManager.Owners owners, boolean atConnector) {
    return ensureArtifactStream(seed, owners, false, false);
  }

  @Override
  public ArtifactStream ensureArtifactStream(
      Randomizer.Seed seed, OwnerManager.Owners owners, boolean atConnector, boolean metadataOnly) {
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();
    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, SettingGenerator.Settings.HARNESS_BAMBOO_CONNECTOR);
    return ensureArtifactStream(seed,
        BambooArtifactStream.builder()
            .appId(atConnector ? GLOBAL_APP_ID : application.getUuid())
            .serviceId(atConnector ? settingAttribute.getUuid() : getServiceId(service))
            .autoPopulate(false)
            .metadataOnly(metadataOnly)
            .name(metadataOnly ? "bamboo-harness-samples-metadataOnly" : "bamboo-harness-samples")
            .sourceName(settingAttribute.getName())
            .jobname("TOD-TOD")
            .artifactPaths(asList("artifacts/todolist.war"))
            .settingId(settingAttribute.getUuid())
            .build(),
        owners);
  }

  private String getServiceId(Service service) {
    return service != null ? service.getUuid() : null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(
      Randomizer.Seed seed, ArtifactStream artifactStream, OwnerManager.Owners owners) {
    BambooArtifactStream bambooArtifactStream = (BambooArtifactStream) artifactStream;
    final BambooArtifactStreamBuilder builder = BambooArtifactStream.builder();

    builder.appId(artifactStream.getAppId());
    builder.serviceId(artifactStream.getServiceId());
    builder.name(artifactStream.getName());

    ArtifactStream existing = artifactStreamGeneratorHelper.exists(builder.build());
    if (existing != null) {
      return existing;
    }

    Preconditions.checkNotNull(bambooArtifactStream.getJobname());
    Preconditions.checkNotNull(bambooArtifactStream.getArtifactPaths());
    Preconditions.checkNotNull(artifactStream.getSourceName());
    Preconditions.checkNotNull(artifactStream.getSettingId());

    builder.jobname(bambooArtifactStream.getJobname());
    builder.artifactPaths(bambooArtifactStream.getArtifactPaths());
    builder.sourceName(artifactStream.getSourceName());
    builder.settingId(artifactStream.getSettingId());
    builder.metadataOnly(artifactStream.isMetadataOnly());

    return artifactStreamGeneratorHelper.saveArtifactStream(builder.build(), owners);
  }
}
