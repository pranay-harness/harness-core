package io.harness.cvng.beans.change.event.type;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ChangeCategory {
  @JsonProperty("Deployment") DEPLOYMENT,
  @JsonProperty("Infrastructure") INFRASTRUCTURE,
  @JsonProperty("Alert") ALERTS
}
