package io.harness.repositories.fullSync;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob;

import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(PL)
public interface FullSyncJobRepository extends CrudRepository<GitFullSyncJob, String>, FullSyncJobRepositoryCustom {
  GitFullSyncJob findByAccountIdentifierAndUuid(String accountIdentifier, String uuid);
}
