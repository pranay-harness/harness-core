/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.pipeline.service.yamlschema.approval;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.yaml.schema.beans.PartialSchemaDTO;

@OwnedBy(HarnessTeam.PIPELINE)
public interface ApprovalYamlSchemaService {
  PartialSchemaDTO getApprovalYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope);
}
