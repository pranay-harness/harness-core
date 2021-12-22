package io.harness.pms.pipeline.service.yamlschema;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.JsonUtils;
import io.harness.yaml.schema.beans.PartialSchemaDTO;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class PartialSchemaCacheUtils {
  public PartialSchemaDTO getPartialSchemaDTO(PartialSchemaValue partialSchemaValue) {
    JsonNode node = JsonUtils.readTree(partialSchemaValue.getSchema());
    return PartialSchemaDTO.builder()
        .schema(node)
        .namespace(partialSchemaValue.getNamespace())
        .nodeName(partialSchemaValue.getNodeName())
        .nodeType(partialSchemaValue.getNodeType())
        .moduleType(partialSchemaValue.getModuleType())
        .build();
  }

  public PartialSchemaValue getPartialSchemaValue(PartialSchemaDTO partialSchemaDTO) {
    return PartialSchemaValue.builder()
        .schema(partialSchemaDTO.getSchema().toString())
        .namespace(partialSchemaDTO.getNamespace())
        .nodeName(partialSchemaDTO.getNodeName())
        .nodeType(partialSchemaDTO.getNodeType())
        .moduleType(partialSchemaDTO.getModuleType())
        .build();
  }
}
