package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.entities.InstanceData;

import java.time.Instant;
import java.util.List;

public interface InstanceDataService {
  boolean create(InstanceData instanceData);

  boolean updateInstanceState(InstanceData instanceData, Instant instant, InstanceState instanceState);

  InstanceData fetchActiveInstanceData(String accountId, String instanceId, List<InstanceState> instanceState);

  InstanceData fetchInstanceData(String accountId, String instanceId);
}
