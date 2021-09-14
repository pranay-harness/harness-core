/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;

import com.fasterxml.jackson.databind.JsonNode;

@OwnedBy(PIPELINE)
public interface NGTriggerYamlSchemaService {
  JsonNode getTriggerYamlSchema(String projectIdentifier, String orgIdentifier, String identifier, Scope scope);
}
