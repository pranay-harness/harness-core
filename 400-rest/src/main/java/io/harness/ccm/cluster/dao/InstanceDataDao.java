package io.harness.ccm.cluster.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.commons.entities.InstanceData;
import io.harness.ccm.commons.entities.InstanceData.InstanceDataKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
@Singleton
@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
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
    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class, excludeAuthority)
                                    .field(InstanceDataKeys.instanceId)
                                    .in(instanceIds);
    return fetchInstanceData(query.fetch().iterator());
  }

  public List<InstanceData> fetchInstanceDataForGivenInstances(
      String accountId, String clusterId, List<String> instanceIds) {
    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class, excludeAuthority)
                                    .field(InstanceDataKeys.accountId)
                                    .equal(accountId)
                                    .field(InstanceDataKeys.clusterId)
                                    .equal(clusterId)
                                    .field(InstanceDataKeys.instanceId)
                                    .in(instanceIds);
    return fetchInstanceData(query.fetch().iterator());
  }

  private List<InstanceData> fetchInstanceData(Iterator<InstanceData> iterator) {
    List<InstanceData> instanceData = new ArrayList<>();
    while (iterator.hasNext()) {
      instanceData.add(iterator.next());
    }
    return instanceData;
  }
}
