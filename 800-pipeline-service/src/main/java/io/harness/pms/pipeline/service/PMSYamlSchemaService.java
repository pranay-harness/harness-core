package io.harness.pms.pipeline.service;

import io.harness.encryption.Scope;

import com.fasterxml.jackson.databind.JsonNode;

public interface PMSYamlSchemaService {
  JsonNode getPipelineYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope);
}
