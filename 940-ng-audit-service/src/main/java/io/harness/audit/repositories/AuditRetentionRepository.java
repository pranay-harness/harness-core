package io.harness.audit.repositories;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.entities.AuditSettings;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface AuditRetentionRepository extends PagingAndSortingRepository<AuditSettings, String> {
  Optional<AuditSettings> findByAccountIdentifier(String accountIdentifier);
}