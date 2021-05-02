package io.harness.instancesync.repository.instancesyncperpetualtask;

import io.harness.instancesync.dto.Instance;

import java.util.List;

public interface InstanceRepository {
  List<Instance> getActiveInstancesByAccount(String accountId, long timestamp);
}
