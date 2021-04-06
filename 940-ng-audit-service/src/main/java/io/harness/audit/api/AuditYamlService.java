package io.harness.audit.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.entities.YamlDiffRecord;

@OwnedBy(PL)
public interface AuditYamlService {
  YamlDiffRecord get(String auditId);
  YamlDiffRecord save(YamlDiffRecord yamlDiffRecord);
  boolean delete(String auditId);
}
