package io.harness.repositories.instance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.instance.Instance;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public interface InstanceRepository {
  List<Instance> getActiveInstancesByAccount(String accountIdentifier, long timestamp);

  List<Instance> getInstances(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String infrastructureMappingId);
}
