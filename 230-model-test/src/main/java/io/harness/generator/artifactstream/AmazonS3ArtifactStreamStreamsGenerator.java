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
import io.harness.generator.Randomizer;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;

import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.AmazonS3ArtifactStream.AmazonS3ArtifactStreamBuilder;
import software.wings.beans.artifact.ArtifactStream;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;

@OwnedBy(CDP)
@Singleton
public class AmazonS3ArtifactStreamStreamsGenerator implements ArtifactStreamsGenerator {
  @Inject private SettingGenerator settingGenerator;
  @Inject private ArtifactStreamGeneratorHelper artifactStreamGeneratorHelper;

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners) {
    return ensureArtifactStream(seed, owners, false);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, String serviceName, boolean atConnector) {
    return null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector) {
    return ensureArtifactStream(seed, owners, atConnector, true);
  }

  private String getServiceId(Service service) {
    return service != null ? service.getUuid() : null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector, boolean metadataOnly) {
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();
    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.AWS_TEST_CLOUD_PROVIDER);

    return ensureArtifactStream(seed,
        AmazonS3ArtifactStream.builder()
            .appId(atConnector ? GLOBAL_APP_ID : application.getUuid())
            .serviceId(atConnector ? settingAttribute.getUuid() : getServiceId(service))
            .name("harness-iis-app")
            .sourceName(settingAttribute.getName())
            .jobname("iis-app-example")
            .artifactPaths(Collections.singletonList("todolist-v*.zip"))
            .settingId(settingAttribute.getUuid())
            .build(),
        owners);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Randomizer.Seed seed, ArtifactStream artifactStream, Owners owners) {
    AmazonS3ArtifactStream amazonS3ArtifactStream = (AmazonS3ArtifactStream) artifactStream;
    final AmazonS3ArtifactStreamBuilder s3ArtifactStreamBuilder = AmazonS3ArtifactStream.builder();

    if (artifactStream != null && artifactStream.getAppId() != null) {
      s3ArtifactStreamBuilder.appId(artifactStream.getAppId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getServiceId() != null) {
      s3ArtifactStreamBuilder.serviceId(artifactStream.getServiceId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getName() != null) {
      s3ArtifactStreamBuilder.name(artifactStream.getName());
    } else {
      throw new UnsupportedOperationException();
    }

    ArtifactStream existingArtifactStream = artifactStreamGeneratorHelper.exists(s3ArtifactStreamBuilder.build());
    if (existingArtifactStream != null) {
      return existingArtifactStream;
    }

    if (amazonS3ArtifactStream.getJobname() != null) {
      s3ArtifactStreamBuilder.jobname(amazonS3ArtifactStream.getJobname());
    } else {
      throw new UnsupportedOperationException();
    }

    if (amazonS3ArtifactStream.getArtifactPaths() != null) {
      s3ArtifactStreamBuilder.artifactPaths(amazonS3ArtifactStream.getArtifactPaths());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getSourceName() != null) {
      s3ArtifactStreamBuilder.sourceName(artifactStream.getSourceName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getSettingId() != null) {
      s3ArtifactStreamBuilder.settingId(artifactStream.getSettingId());
    } else {
      throw new UnsupportedOperationException();
    }

    return artifactStreamGeneratorHelper.saveArtifactStream(s3ArtifactStreamBuilder.build(), owners);
  }
}
