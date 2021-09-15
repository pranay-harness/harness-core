/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.mappers.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.config.ArtifactoryConfig;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

@OwnedBy(HarnessTeam.CDC)
public class ArtifactoryConfigToArtifactoryRequestMapper {
  public static ArtifactoryConfigRequest toArtifactoryRequest(
      ArtifactoryConfig config, EncryptionService encryptionService, List<EncryptedDataDetail> encryptedDetails) {
    if (config.hasCredentials()) {
      encryptionService.decrypt(config, encryptedDetails, false);
    }
    return ArtifactoryConfigRequest.builder()
        .artifactoryUrl(config.getArtifactoryUrl())
        .username(config.getUsername())
        .password(config.getPassword())
        .hasCredentials(config.hasCredentials())
        .build();
  }
}
