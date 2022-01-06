package io.harness.repositories.fullSync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(DX)
public interface GitFullSyncEntityRepository
    extends CrudRepository<GitFullSyncEntityInfo, String>, GitFullSyncEntityRepositoryCustom {
  List<GitFullSyncEntityInfo> findByAccountIdentifierAndMessageId(String accountIdentifier, String messageId);
}
