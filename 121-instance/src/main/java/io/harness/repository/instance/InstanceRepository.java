package io.harness.repository.instance;

import io.harness.dto.instance.Instance;

import java.util.List;

public interface InstanceRepository {
  List<Instance> getActiveInstancesByAccount(String accountId, long timestamp);
}
