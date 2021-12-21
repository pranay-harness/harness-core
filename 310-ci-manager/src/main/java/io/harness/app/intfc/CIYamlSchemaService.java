package io.harness.app.intfc;

import io.harness.encryption.Scope;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;

import java.util.List;

public interface CIYamlSchemaService {
  PartialSchemaDTO getIntegrationStageYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope);
  PartialSchemaDTO getMergedIntegrationStageYamlSchema(
      String projectIdentifier, String orgIdentifier, Scope scope, List<YamlSchemaWithDetails> stepSchemaWithDetails);
  List<YamlSchemaWithDetails> getIntegrationStageYamlSchemaWithDetails(
      String projectIdentifier, String orgIdentifier, Scope scope);
}
