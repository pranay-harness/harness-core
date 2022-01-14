package io.harness.pms.pipeline.service.yamlschema.cache;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class PartialSchemaDTOValue {
  String schema;
  String nodeType;
  String nodeName;
  String namespace;
  ModuleType moduleType;
}
