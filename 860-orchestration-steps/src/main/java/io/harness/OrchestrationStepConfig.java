/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CF)
@Data
@Builder
public class OrchestrationStepConfig {
  @JsonProperty("ffServerBaseUrl") private String ffServerBaseUrl;
  @JsonProperty("ffServerApiKey") private String ffServerApiKey;
}
