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
  ModuleType moduleType;
}
