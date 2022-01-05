package io.harness.cvng.core.services.api;

import io.harness.EntityType;
import io.harness.encryption.Scope;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface CVNGYamlSchemaService {
  List<PartialSchemaDTO> getDeploymentStageYamlSchema(String orgIdentifier, String projectIdentifier, Scope scope);
  PartialSchemaDTO getMergedDeploymentStageYamlSchema(
      String projectIdentifier, String orgIdentifier, Scope scope, List<YamlSchemaWithDetails> stepSchemaWithDetails);
  List<YamlSchemaWithDetails> getDeploymentStageYamlSchemaWithDetails(
      String projectIdentifier, String orgIdentifier, Scope scope);
  JsonNode getStepYamlSchema(EntityType entityType);
}
