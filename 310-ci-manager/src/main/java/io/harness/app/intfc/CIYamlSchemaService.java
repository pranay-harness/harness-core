package io.harness.app.intfc;

import io.harness.encryption.Scope;
import io.harness.yaml.schema.beans.PartialSchemaDTO;

public interface CIYamlSchemaService {
  PartialSchemaDTO getIntegrationStageYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope);
}
