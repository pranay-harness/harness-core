package io.harness.batch.processing.dao.impl;

import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@Slf4j
public class InstanceDataDaoImpl implements InstanceDataDao {
  @Autowired private HPersistence hPersistence;

  @Override
  public boolean create(InstanceData instanceData) {
    return hPersistence.save(instanceData) != null;
  }

  @Override
  public boolean updateInstanceState(
      InstanceData instanceData, Instant instant, String instantField, InstanceState instanceState) {
    UpdateOperations<InstanceData> instanceDataUpdateOperations =
        hPersistence.createUpdateOperations(InstanceData.class)
            .set(instantField, instant)
            .set(InstanceDataKeys.instanceState, instanceState);

    UpdateResults updateResults = hPersistence.update(instanceData, instanceDataUpdateOperations);
    logger.debug("Updated instance state results {} ", updateResults);
    return updateResults.getUpdatedCount() > 0;
  }

  @Override
  public InstanceData fetchActiveInstanceData(String accountId, String instanceId, List<InstanceState> instanceState) {
    return hPersistence.createQuery(InstanceData.class)
        .filter(InstanceDataKeys.accountId, accountId)
        .filter(InstanceDataKeys.instanceId, instanceId)
        .field(InstanceDataKeys.instanceState)
        .in(instanceState)
        .get();
  }

  @Override
  public InstanceData fetchInstanceData(String accountId, String instanceId) {
    return hPersistence.createQuery(InstanceData.class)
        .filter(InstanceDataKeys.accountId, accountId)
        .filter(InstanceDataKeys.instanceId, instanceId)
        .get();
  }

  /**
   * fetching only those instances which were started before given time and are still active
   */
  @Override
  public List<InstanceData> fetchClusterActiveInstanceData(
      String accountId, String clusterName, List<InstanceState> instanceState, Instant startTime) {
    return hPersistence.createQuery(InstanceData.class)
        .filter(InstanceDataKeys.accountId, accountId)
        .filter(InstanceDataKeys.clusterName, clusterName)
        .field(InstanceDataKeys.instanceState)
        .in(instanceState)
        .field(InstanceDataKeys.usageStartTime)
        .lessThanOrEq(startTime)
        .asList();
  }
}
