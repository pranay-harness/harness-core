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
}
