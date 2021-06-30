package io.harness.pms.pipeline.service.yamlschema.approval;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.yaml.schema.beans.PartialSchemaDTO;

@OwnedBy(HarnessTeam.PIPELINE)
public interface ApprovalYamlSchemaService {
  PartialSchemaDTO getApprovalYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope);
}
