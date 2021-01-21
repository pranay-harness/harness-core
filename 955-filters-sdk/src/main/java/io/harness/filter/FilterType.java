package io.harness.filter;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FilterType {
  @JsonProperty("Connector") CONNECTOR,
  @JsonProperty("PipelineSetup") PIPELINE_SETUP,
  @JsonProperty("PipelineExecution") PIPELINE_EXECUTION,
  @JsonProperty("Deployment") DEPLOYMENT
}
