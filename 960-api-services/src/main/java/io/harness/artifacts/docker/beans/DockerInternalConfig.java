/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.artifacts.docker.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString(exclude = "password")
@OwnedBy(HarnessTeam.CDC)
public class DockerInternalConfig {
  String dockerRegistryUrl;
  String username;
  String password;
  boolean isCertValidationRequired;

  public boolean hasCredentials() {
    return isNotEmpty(username);
  }

  public String getDockerRegistryUrl() {
    return dockerRegistryUrl.endsWith("/") ? dockerRegistryUrl : dockerRegistryUrl.concat("/");
  }
}
