package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum MonitoredServiceType {
  @JsonProperty("Application") APPLICATION,
  @JsonProperty("Infrastructure") INFRASTRUCTURE
}
