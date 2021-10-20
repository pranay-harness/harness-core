package io.harness.cvng.servicelevelobjective.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SLIType {
  @JsonProperty("Availability") AVAILABILITY,
  @JsonProperty("Latency") LATENCY

}
