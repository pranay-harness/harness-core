package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.billing.timeseries.data.InstanceLifecycleInfo;
import io.harness.batch.processing.billing.timeseries.data.PrunedInstanceData;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface InstanceDataService {
  boolean create(InstanceData instanceData);

  boolean updateInstanceState(InstanceData instanceData, Instant instant, InstanceState instanceState);

  InstanceData fetchActiveInstanceData(
      String accountId, String clusterId, String instanceId, List<InstanceState> instanceState);

  InstanceData fetchInstanceData(String accountId, String instanceId);

  InstanceData fetchInstanceData(String instanceId);

  InstanceData fetchInstanceData(String accountId, String clusterId, String instanceId);

  InstanceData fetchInstanceDataWithName(String accountId, String clusterId, String instanceId, Long occurredAt);

  PrunedInstanceData fetchPrunedInstanceDataWithName(
      String accountId, String clusterId, String instanceId, Long occurredAt);

  List<InstanceData> fetchClusterActiveInstanceData(String accountId, String clusterId, Instant startTime);

  Set<String> fetchClusterActiveInstanceIds(String accountId, String clusterId, Instant startTime);

  InstanceData getActiveInstance(String accountId, Instant startTime, Instant endTime, CloudProvider cloudProvider);

  List<InstanceLifecycleInfo> fetchInstanceDataForGivenInstances(Set<String> instanceIds);
}
