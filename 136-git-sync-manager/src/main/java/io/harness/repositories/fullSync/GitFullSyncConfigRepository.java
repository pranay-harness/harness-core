package io.harness.repositories.fullSync;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncConfig;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.PL)
public interface GitFullSyncConfigRepository extends CrudRepository<GitFullSyncConfig, String> {
  Optional<GitFullSyncConfig> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
