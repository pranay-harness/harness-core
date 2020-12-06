package io.harness.ccm.cluster;

import io.harness.ccm.cluster.dao.InstanceDataDao;
import io.harness.ccm.commons.entities.InstanceData;

import com.google.inject.Inject;
import java.util.List;

public class InstanceDataServiceImpl implements InstanceDataService {
  @Inject InstanceDataDao instanceDataDao;

  @Override
  public InstanceData get(String instanceId) {
    return instanceDataDao.get(instanceId);
  }

  @Override
  public List<InstanceData> fetchInstanceDataForGivenInstances(List<String> instanceIds) {
    return instanceDataDao.fetchInstanceDataForGivenInstances(instanceIds);
  }

  @Override
  public List<InstanceData> fetchInstanceDataForGivenInstances(
      String accountId, String clusterId, List<String> instanceIds) {
    return instanceDataDao.fetchInstanceDataForGivenInstances(accountId, clusterId, instanceIds);
  }
}
