package io.harness.pms.pipeline.service.yamlschema;

import io.harness.EntityType;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.YamlSchemaDetailsWrapper;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface SchemaGetter {
  List<PartialSchemaDTO> getSchema(List<YamlSchemaWithDetails> yamlSchemaWithDetailsLis);
  YamlSchemaDetailsWrapper getSchemaDetails();
  JsonNode fetchStepYamlSchema(EntityType entityType);
}
