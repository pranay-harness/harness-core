/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.pipeline.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PMSYamlSchemaService {
  JsonNode getPipelineYamlSchema(String accountIdentifier, String projectIdentifier, String orgIdentifier, Scope scope);

  void validateYamlSchema(String accountId, String orgId, String projectId, String yaml);

  void validateUniqueFqn(String yaml) throws IOException;

  void invalidateAllCache();
}
