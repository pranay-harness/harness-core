package io.harness.repositories.syncstatus;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.SyncStatus;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@OwnedBy(HarnessTeam.DX)
@HarnessRepo
public interface SyncStatusRepository extends CrudRepository<SyncStatus, String> {
  Optional<SyncStatus> findByOrgIdentifierAndProjectIdentifierAndServiceIdAndEnvIdAndInfrastructureMappingId(
      String orgIdentifier, String projectIdentifier, String serviceIdentifier, String envIdentifier,
      String infrastructureMappingId);
}
