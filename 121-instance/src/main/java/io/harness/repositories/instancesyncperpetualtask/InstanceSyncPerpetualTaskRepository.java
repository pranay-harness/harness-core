package io.harness.repositories.instancesyncperpetualtask;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface InstanceSyncPerpetualTaskRepository
    extends CrudRepository<InstanceSyncPerpetualTaskInfo, String>, InstanceSyncPerpetualTaskRepositoryCustom {
  Optional<InstanceSyncPerpetualTaskInfo> findByInfrastructureMappingId(String infrastructureMappingId);

  void deleteByInfrastructureMappingId(String infrastructureMappingId);

  void deleteByAccountIdentifierAndId(String accountIdentifier, String id);
}
