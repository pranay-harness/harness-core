package io.harness.cvng.core.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ChangeCategory {
  @JsonProperty("Deployment") DEPLOYMENT,
  @JsonProperty("Infrastructure") INFRASTRUCTURE,
  @JsonProperty("Alert") ALERTS
}
