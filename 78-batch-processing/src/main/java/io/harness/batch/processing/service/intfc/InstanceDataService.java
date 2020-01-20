package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.billing.timeseries.data.PrunedInstanceData;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.entities.InstanceData;

import java.time.Instant;
import java.util.List;

public interface InstanceDataService {
  boolean create(InstanceData instanceData);

  boolean updateInstanceState(InstanceData instanceData, Instant instant, InstanceState instanceState);

  InstanceData fetchActiveInstanceData(
      String accountId, String clusterId, String instanceId, List<InstanceState> instanceState);

  InstanceData fetchInstanceData(String accountId, String instanceId);

  InstanceData fetchInstanceDataWithName(String accountId, String settingId, String instanceId, Long occurredAt);

  PrunedInstanceData fetchPrunedInstanceDataWithName(
      String accountId, String settingId, String instanceId, Long occurredAt);

  List<InstanceData> fetchClusterActiveInstanceData(String accountId, String clusterId, Instant startTime);
}
