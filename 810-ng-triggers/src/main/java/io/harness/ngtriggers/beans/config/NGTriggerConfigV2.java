/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.beans.config;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.NGTriggerSourceV2;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PIPELINE)
public class NGTriggerConfigV2 implements NGTriggerInterface {
  String name;
  @NotNull String identifier;
  String description;
  String orgIdentifier;
  String projectIdentifier;
  String pipelineIdentifier;
  Map<String, String> tags;
  String inputYaml;
  NGTriggerSourceV2 source;
  @Builder.Default Boolean enabled = Boolean.TRUE;
}
