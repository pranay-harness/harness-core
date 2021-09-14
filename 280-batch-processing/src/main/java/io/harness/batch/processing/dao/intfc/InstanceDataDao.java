/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.dao.intfc;

import io.harness.ccm.commons.beans.InstanceState;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.entities.batch.InstanceData;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface InstanceDataDao {
  boolean create(InstanceData instanceData);

  boolean updateInstanceStopTime(InstanceData instanceData, Instant stopTime);

  InstanceData fetchInstanceData(String instanceId);

  List<InstanceData> fetchInstanceData(Set<String> instanceIds);

  InstanceData fetchActiveInstanceData(
      String accountId, String clusterId, String instanceId, List<InstanceState> instanceState);

  List<InstanceData> fetchActivePVList(String accountId, Instant startTime, Instant endTime);

  void updateInstanceActiveIterationTime(InstanceData instanceData);

  InstanceData fetchInstanceData(String accountId, String instanceId);

  InstanceData fetchInstanceData(String accountId, String clusterId, String instanceId);

  InstanceData fetchInstanceDataWithName(String accountId, String settingId, String instanceName, Long occurredAt);

  List<InstanceData> fetchClusterActiveInstanceData(
      String accountId, String clusterName, List<InstanceState> instanceState, Instant startTime);

  Set<String> fetchClusterActiveInstanceIds(
      String accountId, String clusterName, List<InstanceState> instanceState, Instant startTime);

  List<InstanceData> fetchClusterActiveInstanceData(
      String accountId, String clusterName, List<InstanceType> instanceTypes, InstanceState instanceState);

  InstanceData getK8sPodInstance(String accountId, String clusterId, String namespace, String podName);

  List<InstanceData> fetchInstanceDataForGivenInstances(String accountId, String clusterId, List<String> instanceIds);

  List<InstanceData> getInstanceDataListsOfTypes(
      String accountId, int batchSize, Instant startTime, Instant endTime, List<InstanceType> instanceTypes);
}
