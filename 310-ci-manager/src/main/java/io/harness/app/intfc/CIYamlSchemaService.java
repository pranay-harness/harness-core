package io.harness.app.intfc;

import io.harness.encryption.Scope;

import com.fasterxml.jackson.databind.JsonNode;

public interface CIYamlSchemaService {
  JsonNode getIntegrationStageYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope);
}
