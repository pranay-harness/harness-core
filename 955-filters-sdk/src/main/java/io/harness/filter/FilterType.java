package io.harness.filter;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(DX)
public enum FilterType {
  @JsonProperty("Connector") CONNECTOR,
  @JsonProperty("DelegateProfile") DELEGATEPROFILE,
  @JsonProperty("Delegate") DELEGATE,
  @JsonProperty("PipelineSetup") PIPELINESETUP,
  @JsonProperty("PipelineExecution") PIPELINEEXECUTION,
  @JsonProperty("Deployment") DEPLOYMENT,
  @JsonProperty("Audit") AUDIT,
  @JsonProperty("Template") TEMPLATE
}
