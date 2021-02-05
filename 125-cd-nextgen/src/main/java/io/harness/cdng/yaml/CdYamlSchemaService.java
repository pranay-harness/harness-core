package io.harness.cdng.yaml;

import io.harness.encryption.Scope;

import com.fasterxml.jackson.databind.JsonNode;

public interface CdYamlSchemaService {
  JsonNode getDeploymentStageYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope);
}
