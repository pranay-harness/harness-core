package io.harness.filter;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FilterType {
  @JsonProperty("Connector") CONNECTOR,
  @JsonProperty("Pipeline") PIPELINE,
  @JsonProperty("Deployment") DEPLOYMENT
}
