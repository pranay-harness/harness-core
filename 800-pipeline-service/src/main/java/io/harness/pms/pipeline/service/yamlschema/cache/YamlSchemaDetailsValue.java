package io.harness.pms.pipeline.service.yamlschema.cache;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.schema.beans.YamlSchemaMetadata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class YamlSchemaDetailsValue {
  String schema;
  String schemaClassName;
  ModuleType moduleType;
  YamlSchemaMetadata yamlSchemaMetadata;
  boolean isAvailableAtOrgLevel;
  boolean isAvailableAtAccountLevel;
  boolean isAvailableAtProjectLevel;
}
