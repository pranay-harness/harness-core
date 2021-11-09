package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.LocalEncryptionMigrationInfo;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@OwnedBy(PL)
@HarnessRepo
public interface LocalEncryptionMigrationInfoRepository extends CrudRepository<LocalEncryptionMigrationInfo, String> {
  Optional<LocalEncryptionMigrationInfo> findByAccountId(String accountId);
}
