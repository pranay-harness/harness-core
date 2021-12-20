package io.harness.subscription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TierMode {
  @JsonProperty("volume") VOLUME,
  @JsonProperty("graduated") GRADUATED;
}
