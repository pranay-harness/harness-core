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
import io.harness.generator.GeneratorUtils;
import io.harness.generator.OwnerManager.Owners;

import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(CDP)
@Singleton
public class ArtifactStreamGeneratorHelper {
  @Inject WingsPersistence wingsPersistence;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject ServiceResourceService serviceResourceService;

  // TODO: ASR: update this method to use setting_id + name after refactoring
  public ArtifactStream exists(ArtifactStream artifactStream) {
    // TODO: ASR: IMP: update this after refactor
    return wingsPersistence.createQuery(ArtifactStream.class)
        .filter(ArtifactStreamKeys.appId, artifactStream.fetchAppId())
        .filter(ArtifactStreamKeys.serviceId, artifactStream.getServiceId())
        .filter(ArtifactStreamKeys.name, artifactStream.getName())
        .get();
  }

  public ArtifactStream saveArtifactStream(ArtifactStream artifactStream, Owners owners) {
    return GeneratorUtils.suppressDuplicateException(
        () -> createArtifactStream(artifactStream, owners), () -> exists(artifactStream));
  }

  private ArtifactStream createArtifactStream(ArtifactStream artifactStream, Owners owners) {
    ArtifactStream savedArtifactStream = artifactStreamService.create(artifactStream, false);
    if (!savedArtifactStream.getAppId().equals(GLOBAL_APP_ID)) {
      Service service = owners.obtainServiceByServiceId(artifactStream.getServiceId());
      if (service == null) {
        return savedArtifactStream;
      }

      // TODO: ASR: update this method after refactor

      Service updatedService = serviceResourceService.addArtifactStreamId(service, savedArtifactStream.getUuid());
      owners.obtainServiceByServiceId(artifactStream.getServiceId())
          .setArtifactStreamIds(updatedService.getArtifactStreamIds());
    }
    return savedArtifactStream;
  }
}
