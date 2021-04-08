package io.harness.audit.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.entities.YamlDiffRecord;

import java.time.Instant;

@OwnedBy(PL)
public interface AuditYamlService {
  YamlDiffRecord get(String auditId);
  YamlDiffRecord save(YamlDiffRecord yamlDiffRecord);
  void purgeYamlDiffOlderThanTimestamp(String accountIdentifier, Instant timestamp);
  boolean delete(String auditId);
}
