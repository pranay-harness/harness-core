package io.harness.repository.instance;

import io.harness.dto.instance.Instance;

import java.util.List;

public interface InstanceRepository {
  List<Instance> getActiveInstancesByAccount(String accountIdentifier, long timestamp);

  List<Instance> getInstances(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String infrastructureMappingId);
}
