package io.harness.cdng.yaml;

import io.harness.encryption.Scope;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;

import java.util.List;

public interface CdYamlSchemaService {
  PartialSchemaDTO getDeploymentStageYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope);
  PartialSchemaDTO getMergedDeploymentStageYamlSchema(
      String projectIdentifier, String orgIdentifier, Scope scope, List<YamlSchemaWithDetails> stepSchemaWithDetails);
  List<YamlSchemaWithDetails> getDeploymentStageYamlSchemaWithDetails(
      String projectIdentifier, String orgIdentifier, Scope scope);
}
