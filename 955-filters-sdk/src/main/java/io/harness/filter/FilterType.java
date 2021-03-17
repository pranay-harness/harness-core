package io.harness.filter;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FilterType {
  @JsonProperty("Connector") CONNECTOR,
  @JsonProperty("PipelineSetup") PIPELINESETUP,
  @JsonProperty("PipelineExecution") PIPELINEEXECUTION,
  @JsonProperty("Deployment") DEPLOYMENT,
  @JsonProperty("Audit") AUDIT
}
