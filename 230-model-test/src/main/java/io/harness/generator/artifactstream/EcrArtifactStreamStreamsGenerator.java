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
import software.wings.beans.artifact.EcrArtifactStream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(CDP)
@Singleton
public class EcrArtifactStreamStreamsGenerator implements ArtifactStreamsGenerator {
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
    ArtifactStream artifactStream = EcrArtifactStream.builder()
                                        .appId(atConnector ? GLOBAL_APP_ID : application.getUuid())
                                        .serviceId(atConnector    ? settingAttribute.getUuid()
                                                : service != null ? service.getUuid()
                                                                  : null)
                                        .region("us-east-1")
                                        .imageName("hello-world")
                                        .name("hello-world")
                                        .autoPopulate(true)
                                        .metadataOnly(true)
                                        .settingId(settingAttribute.getUuid())
                                        .build();
    return ensureArtifactStream(seed, artifactStream, owners);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector, boolean metadataOnly) {
    return null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, ArtifactStream artifactStream, Owners owners) {
    EcrArtifactStream ecrArtifactStream = (EcrArtifactStream) artifactStream;
    ArtifactStream existing = artifactStreamGeneratorHelper.exists(ecrArtifactStream);
    if (existing != null) {
      return existing;
    }
    return artifactStreamGeneratorHelper.saveArtifactStream(EcrArtifactStream.builder()
                                                                .appId(ecrArtifactStream.getAppId())
                                                                .serviceId(ecrArtifactStream.getServiceId())
                                                                .name(ecrArtifactStream.getName())
                                                                .region(ecrArtifactStream.getRegion())
                                                                .imageName(ecrArtifactStream.getImageName())
                                                                .autoPopulate(ecrArtifactStream.isAutoPopulate())
                                                                .settingId(ecrArtifactStream.getSettingId())
                                                                .metadataOnly(true)
                                                                .build(),
        owners);
  }
}
