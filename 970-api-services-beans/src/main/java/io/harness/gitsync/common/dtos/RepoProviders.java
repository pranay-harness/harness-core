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

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static RepoProviders fromString(@JsonProperty("repoProviderName") String repoProvider) {
    for (RepoProviders repoProviderEnum : RepoProviders.values()) {
      if (repoProviderEnum.name().equalsIgnoreCase(repoProvider)) {
        return repoProviderEnum;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + repoProvider);
  }
}
