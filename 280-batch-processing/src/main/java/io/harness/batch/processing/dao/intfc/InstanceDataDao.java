package io.harness.batch.processing.dao.intfc;

import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.ccm.commons.beans.InstanceState;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.entities.InstanceData;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface InstanceDataDao {
  boolean create(InstanceData instanceData);

  boolean updateInstanceStopTime(InstanceData instanceData, Instant stopTime);

  InstanceData fetchInstanceData(String instanceId);

  List<InstanceData> fetchInstanceData(Set<String> instanceIds);

  boolean updateInstanceState(
      InstanceData instanceData, Instant instant, String instantField, InstanceState instanceState);

  InstanceData fetchActiveInstanceData(
      String accountId, String clusterId, String instanceId, List<InstanceState> instanceState);

  List<InstanceData> fetchActivePVList(String accountId, Instant startTime, Instant endTime);

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

  List<InstanceData> getInstanceDataListsOfType(String accountId, int batchSize, Instant startTime, Instant endTime,
      Instant seekingDate, InstanceType instanceType);

  List<InstanceData> getInstanceDataListsOtherThanPV(
      String accountId, int batchSize, Instant startTime, Instant endTime, Instant seekingDate);
}
