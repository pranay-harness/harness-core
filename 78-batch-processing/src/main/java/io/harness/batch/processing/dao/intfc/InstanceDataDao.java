package io.harness.batch.processing.dao.intfc;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;

import java.time.Instant;
import java.util.List;

public interface InstanceDataDao {
  boolean create(InstanceData instanceData);

  InstanceData upsert(InstanceEvent instanceEvent);

  InstanceData upsert(InstanceInfo instanceInfo);

  boolean updateInstanceState(
      InstanceData instanceData, Instant instant, String instantField, InstanceState instanceState);

  InstanceData fetchActiveInstanceData(
      String accountId, String clusterId, String instanceId, List<InstanceState> instanceState);

  InstanceData fetchInstanceData(String accountId, String instanceId);

  InstanceData fetchInstanceData(String accountId, String clusterId, String instanceId);

  InstanceData fetchInstanceDataWithName(String accountId, String settingId, String instanceName, Long occurredAt);

  List<InstanceData> fetchClusterActiveInstanceData(
      String accountId, String clusterName, List<InstanceState> instanceState, Instant startTime);

  InstanceData getActiveInstance(String accountId, Instant startTime, Instant endTime, CloudProvider cloudProvider);

  InstanceData getK8sPodInstance(String accountId, String clusterId, String namespace, String podName);
}
