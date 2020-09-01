package io.harness.gitsync.common.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
