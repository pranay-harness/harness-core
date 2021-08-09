package io.harness.ngtriggers.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;

import com.fasterxml.jackson.databind.JsonNode;

@OwnedBy(PIPELINE)
public interface NGTriggerYamlSchemaService {
  JsonNode getTriggerYamlSchema(String projectIdentifier, String orgIdentifier, String identifier, Scope scope);
}
