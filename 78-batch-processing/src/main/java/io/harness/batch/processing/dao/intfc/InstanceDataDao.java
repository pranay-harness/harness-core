package io.harness.batch.processing.dao.intfc;

import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.entities.InstanceData;

import java.time.Instant;
import java.util.List;

public interface InstanceDataDao {
  boolean create(InstanceData instanceData);

  boolean updateInstanceState(
      InstanceData instanceData, Instant instant, String instantField, InstanceState instanceState);

  InstanceData fetchActiveInstanceData(String accountId, String instanceId, List<InstanceState> instanceState);

  InstanceData fetchInstanceData(String accountId, String instanceId);

  List<InstanceData> fetchClusterActiveInstanceData(
      String accountId, String clusterName, List<InstanceState> instanceState, Instant startTime);
}
