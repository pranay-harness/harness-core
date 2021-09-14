/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
