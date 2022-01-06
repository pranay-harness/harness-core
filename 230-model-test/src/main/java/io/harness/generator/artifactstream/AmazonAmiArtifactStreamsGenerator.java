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
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.AmiArtifactStream.AmiArtifactStreamBuilder;
import software.wings.beans.artifact.ArtifactStream;

import com.google.inject.Inject;
import javax.validation.constraints.NotNull;

@OwnedBy(CDP)
public class AmazonAmiArtifactStreamsGenerator implements ArtifactStreamsGenerator {
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
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();
    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER);

    return ensureArtifactStream(seed,
        AmiArtifactStream.builder()
            .name("aws-playground-ami")
            .appId(atConnector ? GLOBAL_APP_ID : application.getUuid())
            .serviceId(atConnector    ? settingAttribute.getUuid()
                    : service != null ? service.getUuid()
                                      : null)
            .settingId(settingAttribute.getUuid())
            .region("us-east-1")
            .sourceName("us-east-1")
            .autoPopulate(false)
            .build(),
        owners);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector, boolean metadataOnly) {
    return null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(
      Randomizer.Seed seed, @NotNull ArtifactStream artifactStream, Owners owners) {
    AmiArtifactStream amiArtifactStream = (AmiArtifactStream) artifactStream;
    final AmiArtifactStreamBuilder amiArtifactStreamBuilder = AmiArtifactStream.builder();

    if (artifactStream.getAppId() != null) {
      amiArtifactStreamBuilder.appId(amiArtifactStream.getAppId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getServiceId() != null) {
      amiArtifactStreamBuilder.serviceId(amiArtifactStream.getServiceId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getName() != null) {
      amiArtifactStreamBuilder.name(artifactStream.getName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getSourceName() != null) {
      amiArtifactStreamBuilder.sourceName(amiArtifactStream.getSourceName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (amiArtifactStream.getRegion() != null) {
      amiArtifactStreamBuilder.region(amiArtifactStream.getRegion());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getSettingId() != null) {
      amiArtifactStreamBuilder.settingId(amiArtifactStream.getSettingId());
    } else {
      throw new UnsupportedOperationException();
    }

    ArtifactStream existingArtifactStream = artifactStreamGeneratorHelper.exists(amiArtifactStreamBuilder.build());
    if (existingArtifactStream != null) {
      return existingArtifactStream;
    }

    return artifactStreamGeneratorHelper.saveArtifactStream(amiArtifactStreamBuilder.build(), owners);
  }
}
