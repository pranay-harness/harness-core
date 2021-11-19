package io.harness.cvng.servicelevelobjective.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ServiceLevelIndicatorType {
  @JsonProperty("Availability") AVAILABILITY,
  @JsonProperty("Latency") LATENCY

}
