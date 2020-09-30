package io.harness.batch.processing.dao.intfc;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface InstanceDataDao {
  boolean create(InstanceData instanceData);

  boolean updateInstanceStopTime(InstanceData instanceData, Instant stopTime);

  InstanceData upsert(InstanceEvent instanceEvent);

  void upsert(List<InstanceEvent> instanceEvents);

  InstanceData upsert(InstanceInfo instanceInfo);

  InstanceData fetchInstanceData(String instanceId);

  List<InstanceData> fetchInstanceData(Set<String> instanceIds);

  boolean updateInstanceState(
      InstanceData instanceData, Instant instant, String instantField, InstanceState instanceState);

  InstanceData fetchActiveInstanceData(
      String accountId, String clusterId, String instanceId, List<InstanceState> instanceState);

  InstanceData fetchInstanceData(String accountId, String instanceId);

  InstanceData fetchInstanceData(String accountId, String clusterId, String instanceId);

  InstanceData fetchInstanceDataWithName(String accountId, String settingId, String instanceName, Long occurredAt);

  List<InstanceData> fetchClusterActiveInstanceData(
      String accountId, String clusterName, List<InstanceState> instanceState, Instant startTime);

  Set<String> fetchClusterActiveInstanceIds(
      String accountId, String clusterName, List<InstanceState> instanceState, Instant startTime);

  List<InstanceData> fetchClusterActiveInstanceData(
      String accountId, String clusterName, List<InstanceType> instanceTypes, InstanceState instanceState);

  InstanceData getActiveInstance(String accountId, Instant startTime, Instant endTime, CloudProvider cloudProvider);

  InstanceData getK8sPodInstance(String accountId, String clusterId, String namespace, String podName);

  List<InstanceData> fetchInstanceDataForGivenInstances(String accountId, String clusterId, List<String> instanceIds);

  List<InstanceData> getInstanceDataLists(
      String accountId, int batchSize, Instant startTime, Instant endTime, Instant seekingDate);
}
