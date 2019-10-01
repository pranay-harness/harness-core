package io.harness.batch.processing.dao.impl;

import static java.util.Objects.isNull;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
public class InstanceDataDaoImpl implements InstanceDataDao {
  @Autowired private HPersistence hPersistence;

  @Override
  public boolean create(InstanceData instanceData) {
    return hPersistence.save(instanceData) != null;
  }

  @Override
  public InstanceData upsert(InstanceEvent instanceEvent) {
    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class)
                                    .filter(InstanceDataKeys.accountId, instanceEvent.getAccountId())
                                    .filter(InstanceDataKeys.cloudProviderId, instanceEvent.getCloudProviderId())
                                    .filter(InstanceDataKeys.instanceId, instanceEvent.getInstanceId());

    UpdateOperations<InstanceData> updateOperations = hPersistence.createUpdateOperations(InstanceData.class);

    Instant instant = instanceEvent.getTimestamp();
    switch (instanceEvent.getType()) {
      case STOP:
        updateOperations.set(InstanceDataKeys.usageStopTime, instant);
        break;
      case START:
        updateOperations.set(InstanceDataKeys.usageStartTime, instant);
        break;
      default:
        break;
    }
    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(true).returnNew(true);
    return hPersistence.upsert(query, updateOperations, findAndModifyOptions);
  }

  @Override
  public InstanceData upsert(InstanceInfo instanceInfo) {
    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class)
                                    .filter(InstanceDataKeys.accountId, instanceInfo.getAccountId())
                                    .filter(InstanceDataKeys.cloudProviderId, instanceInfo.getCloudProviderId())
                                    .filter(InstanceDataKeys.instanceId, instanceInfo.getInstanceId());
    UpdateOperations<InstanceData> updateOperations =
        hPersistence.createUpdateOperations(InstanceData.class)
            .set(InstanceDataKeys.accountId, instanceInfo.getAccountId())
            .set(InstanceDataKeys.cloudProviderId, instanceInfo.getCloudProviderId())
            .set(InstanceDataKeys.instanceId, instanceInfo.getInstanceId())
            .set(InstanceDataKeys.instanceType, instanceInfo.getInstanceType());

    if (!isNull(instanceInfo.getResource())) {
      updateOperations.set(InstanceDataKeys.totalResource, instanceInfo.getResource());
    }

    if (!isNull(instanceInfo.getLabels())) {
      updateOperations.set(InstanceDataKeys.labels, instanceInfo.getLabels());
    }

    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(true).returnNew(true);

    return hPersistence.upsert(query, updateOperations, findAndModifyOptions);
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
