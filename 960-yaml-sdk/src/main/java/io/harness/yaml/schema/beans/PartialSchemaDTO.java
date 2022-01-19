/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.schema.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
@Schema(name = "PartialSchema", description = "This is the view of the PartialSchema entity defined in Harness")
public class PartialSchemaDTO {
  JsonNode schema;
  String nodeType;
  String nodeName;
  String namespace;
  boolean skipStageSchema;
  ModuleType moduleType;
}
