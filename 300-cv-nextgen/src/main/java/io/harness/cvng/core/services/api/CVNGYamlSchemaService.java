/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.api;

import io.harness.encryption.Scope;
import io.harness.yaml.schema.beans.PartialSchemaDTO;

public interface CVNGYamlSchemaService {
  PartialSchemaDTO getDeploymentStageYamlSchema(String orgIdentifier, String projectIdentifier, Scope scope);
}
