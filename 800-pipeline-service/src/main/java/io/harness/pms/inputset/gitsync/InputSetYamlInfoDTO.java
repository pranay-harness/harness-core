/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.inputset.gitsync;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.plancreator.pipeline.PipelineInfoConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class InputSetYamlInfoDTO {
  @EntityName String name;
  @EntityIdentifier String identifier;

  String description;
  Map<String, String> tags;

  String orgIdentifier;
  String projectIdentifier;

  @JsonProperty("pipeline") PipelineInfoConfig pipelineInfoConfig;
}
