package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.billing.timeseries.data.InstanceLifecycleInfo;
import io.harness.batch.processing.billing.timeseries.data.PrunedInstanceData;
import io.harness.ccm.commons.beans.InstanceState;
import io.harness.ccm.commons.entities.InstanceData;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface InstanceDataService {
  boolean create(InstanceData instanceData);

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

  List<InstanceLifecycleInfo> fetchInstanceDataForGivenInstances(Set<String> instanceIds);
}
