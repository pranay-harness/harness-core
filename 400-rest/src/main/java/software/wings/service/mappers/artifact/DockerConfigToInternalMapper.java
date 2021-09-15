/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.mappers.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.docker.beans.DockerInternalConfig;
import io.harness.data.structure.EmptyPredicate;

import software.wings.beans.DockerConfig;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class DockerConfigToInternalMapper {
  public DockerInternalConfig toDockerInternalConfig(DockerConfig dockerConfig) {
    String password =
        EmptyPredicate.isNotEmpty(dockerConfig.getPassword()) ? new String(dockerConfig.getPassword()) : null;

    return DockerInternalConfig.builder()
        .dockerRegistryUrl(dockerConfig.getDockerRegistryUrl())
        .isCertValidationRequired(dockerConfig.isCertValidationRequired())
        .username(dockerConfig.getUsername())
        .password(password)
        .build();
  }
}
