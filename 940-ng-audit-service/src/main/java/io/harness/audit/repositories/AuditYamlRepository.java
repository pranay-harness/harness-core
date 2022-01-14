package io.harness.audit.repositories;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.entities.YamlDiffRecord;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface AuditYamlRepository
    extends PagingAndSortingRepository<YamlDiffRecord, String>, AuditYamlRepositoryCustom {
  Optional<YamlDiffRecord> findByAuditId(String auditId);
  void deleteByAuditId(String auditId);
}
