package io.harness.pms.pipeline.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;

import com.fasterxml.jackson.databind.JsonNode;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PMSYamlSchemaService {
  JsonNode getPipelineYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope);

  void validateYamlSchema(String orgId, String projectId, String yaml);

  void validateYamlSchema(String accountId, String orgId, String projectId, String yaml);
}
