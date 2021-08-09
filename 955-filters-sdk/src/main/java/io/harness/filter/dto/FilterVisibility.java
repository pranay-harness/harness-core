package io.harness.filter.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(DX)
public enum FilterVisibility {
  @JsonProperty("EveryOne") EVERYONE,
  @JsonProperty("OnlyCreator") ONLY_CREATOR
}
