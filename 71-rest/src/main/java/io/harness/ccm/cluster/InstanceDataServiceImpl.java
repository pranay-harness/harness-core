package io.harness.ccm.cluster;

import com.google.inject.Inject;

import io.harness.ccm.cluster.dao.InstanceDataDao;
import io.harness.ccm.cluster.entities.InstanceData;

import java.util.List;

public class InstanceDataServiceImpl implements InstanceDataService {
  @Inject InstanceDataDao instanceDataDao;

  @Override
  public InstanceData get(String instanceId) {
    return instanceDataDao.get(instanceId);
  }

  @Override
  public List<InstanceData> fetchInstanceDataForGivenInstances(
      String accountId, String clusterId, List<String> instanceIds) {
    return instanceDataDao.fetchInstanceDataForGivenInstances(accountId, clusterId, instanceIds);
  }
}
