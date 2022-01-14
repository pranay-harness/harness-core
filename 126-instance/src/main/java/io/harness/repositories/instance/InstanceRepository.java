package io.harness.repositories.instance;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.Instance;

import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface InstanceRepository extends CrudRepository<Instance, String>, InstanceRepositoryCustom {
  void deleteByInstanceKey(String instanceKey);
}
