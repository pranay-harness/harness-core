/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(DX)
public enum RepoProviders {
  @JsonProperty("github") GITHUB,
  @JsonProperty("gitlab") GITLAB,
  @JsonProperty("bitbucket") BITBUCKET,
  @JsonProperty("unknown") UNKNOWN;

  @JsonCreator
  public static RepoProviders fromString(@JsonProperty("repoProviderName") String repoProvider) {
    for (RepoProviders repoProviderEnum : RepoProviders.values()) {
      if (repoProviderEnum.name().equalsIgnoreCase(repoProvider)) {
        return repoProviderEnum;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + repoProvider);
  }
}
