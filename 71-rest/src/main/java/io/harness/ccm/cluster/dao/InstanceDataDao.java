package io.harness.ccm.cluster.dao;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.entities.InstanceData;
import io.harness.ccm.cluster.entities.InstanceData.InstanceDataKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Singleton
public class InstanceDataDao {
  @Inject private HPersistence hPersistence;

  public boolean create(InstanceData instanceData) {
    return hPersistence.save(instanceData) != null;
  }

  public InstanceData get(String instanceId) {
    return hPersistence.createQuery(InstanceData.class, excludeAuthority)
        .filter(InstanceDataKeys.instanceId, instanceId)
        .get();
  }

  public List<InstanceData> fetchInstanceDataForGivenInstances(List<String> instanceIds) {
    return hPersistence.createQuery(InstanceData.class, excludeAuthority)
        .field(InstanceDataKeys.instanceId)
        .in(instanceIds)
        .asList();
  }

  public List<InstanceData> fetchInstanceDataForGivenInstances(
      String accountId, String clusterId, List<String> instanceIds) {
    return hPersistence.createQuery(InstanceData.class, excludeAuthority)
        .field(InstanceDataKeys.accountId)
        .equal(accountId)
        .field(InstanceDataKeys.clusterId)
        .equal(clusterId)
        .field(InstanceDataKeys.instanceId)
        .in(instanceIds)
        .asList();
  }
}
