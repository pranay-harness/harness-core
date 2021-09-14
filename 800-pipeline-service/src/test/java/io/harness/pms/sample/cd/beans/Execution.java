/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sample.cd.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.YamlNode;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@NoArgsConstructor
public class Execution {
  @JsonProperty(YamlNode.UUID_FIELD_NAME) String uuid;
  List<JsonNode> steps;
}
