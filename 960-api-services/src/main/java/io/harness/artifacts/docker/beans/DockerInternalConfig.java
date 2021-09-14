/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
