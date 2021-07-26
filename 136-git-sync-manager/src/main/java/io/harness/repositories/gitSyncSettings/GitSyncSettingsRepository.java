package io.harness.repositories.gitSyncSettings;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitSyncSettings;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface GitSyncSettingsRepository
    extends CrudRepository<GitSyncSettings, String>, GitSyncSettingsCustomRepository {
  Optional<GitSyncSettings> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
