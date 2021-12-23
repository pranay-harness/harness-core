package io.harness.yaml.schema.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
@OwnedBy(DX)
public class YamlSchemaWithDetails {
  JsonNode schema;
  String schemaClassName;
  ModuleType moduleType;
  YamlSchemaMetadata yamlSchemaMetadata;
  boolean isAvailableAtOrgLevel;
  boolean isAvailableAtAccountLevel;
  boolean isAvailableAtProjectLevel;
}
