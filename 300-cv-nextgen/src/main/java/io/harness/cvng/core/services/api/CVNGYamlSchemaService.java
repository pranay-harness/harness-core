package io.harness.cvng.core.services.api;

import io.harness.encryption.Scope;
import io.harness.yaml.schema.beans.PartialSchemaDTO;

public interface CVNGYamlSchemaService {
  PartialSchemaDTO getDeploymentStageYamlSchema(String orgIdentifier, String projectIdentifier, Scope scope);
}
