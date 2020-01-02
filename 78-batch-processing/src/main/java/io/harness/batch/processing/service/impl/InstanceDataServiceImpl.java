package io.harness.batch.processing.service.impl;

import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class InstanceDataServiceImpl implements InstanceDataService {
  @Autowired private InstanceDataDao instanceDataDao;

  @Override
  public boolean create(InstanceData instanceData) {
    return instanceDataDao.create(instanceData);
  }

  @Override
  public boolean updateInstanceState(InstanceData instanceData, Instant instant, InstanceState instanceState) {
    String instantField = null;
    if (InstanceState.RUNNING == instanceState) {
      instantField = InstanceDataKeys.usageStartTime;
    } else if (InstanceState.STOPPED == instanceState) {
      instantField = InstanceDataKeys.usageStopTime;
    }

    if (null != instantField) {
      return instanceDataDao.updateInstanceState(instanceData, instant, instantField, instanceState);
    }
    return false;
  }

  @Override
  public InstanceData fetchActiveInstanceData(String accountId, String instanceId, List<InstanceState> instanceState) {
    return instanceDataDao.fetchActiveInstanceData(accountId, instanceId, instanceState);
  }

  @Override
  public InstanceData fetchInstanceData(String accountId, String instanceId) {
    return instanceDataDao.fetchInstanceData(accountId, instanceId);
  }

  @Override
  public InstanceData fetchInstanceDataWithName(
      String accountId, String settingId, String instanceId, Long occurredAt) {
    return instanceDataDao.fetchInstanceDataWithName(accountId, settingId, instanceId, occurredAt);
  }

  @Override
  public List<InstanceData> fetchClusterActiveInstanceData(
      String accountId, String settingId, String clusterId, Instant startTime) {
    List<InstanceState> instanceStates =
        new ArrayList<>(Arrays.asList(InstanceState.INITIALIZING, InstanceState.RUNNING));
    return instanceDataDao.fetchClusterActiveInstanceData(accountId, settingId, clusterId, instanceStates, startTime);
  }
}
