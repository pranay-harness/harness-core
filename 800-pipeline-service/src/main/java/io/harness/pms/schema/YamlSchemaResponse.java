package io.harness.pms.schema;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
public class YamlSchemaResponse {
  JsonNode schema;
  SchemaErrorResponse schemaErrorResponse;
}
