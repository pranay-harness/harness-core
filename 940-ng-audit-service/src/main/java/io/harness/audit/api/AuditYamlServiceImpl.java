package io.harness.audit.api;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.entities.YamlDiffRecord;
import io.harness.audit.repositories.AuditYamlRepository;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import java.util.Optional;

@OwnedBy(PL)
public class AuditYamlServiceImpl implements AuditYamlService {
  @Inject private AuditYamlRepository auditYamlRepository;

  public YamlDiffRecord get(String auditId) {
    if (isNotEmpty(auditId)) {
      Optional<YamlDiffRecord> yamlDiffRecord = auditYamlRepository.findByAuditId(auditId);
      if (yamlDiffRecord.isPresent()) {
        return yamlDiffRecord.get();
      }
    }
    throw new InvalidRequestException("Audit with id {} does not exist".format(auditId));
  }

  public YamlDiffRecord save(YamlDiffRecord yamlDiffRecord) {
    return auditYamlRepository.save(yamlDiffRecord);
  }

  public YamlDiffRecord delete(String auditId) {
    if (isNotEmpty(auditId)) {
      auditYamlRepository.deleteByAuditId(auditId);
    }
    throw new InvalidRequestException("Audit with id {} does not exist".format(auditId));
  }
}
