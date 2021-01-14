package io.harness.yaml.schema.beans;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PartialSchemaDTO {
  JsonNode schema;
  String nodeType;
  String nodeName;
}
