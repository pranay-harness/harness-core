package io.harness.batch.processing.dao.impl;

import static java.util.Objects.isNull;

import com.google.inject.Inject;

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
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
public class InstanceDataDaoImpl implements InstanceDataDao {
  @Autowired @Inject private HPersistence hPersistence;

  @Override
  public boolean create(InstanceData instanceData) {
    return hPersistence.save(instanceData) != null;
  }

  @Override
  public InstanceData upsert(InstanceEvent instanceEvent) {
    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class)
                                    .filter(InstanceDataKeys.accountId, instanceEvent.getAccountId())
                                    .filter(InstanceDataKeys.clusterId, instanceEvent.getClusterId())
                                    .filter(InstanceDataKeys.instanceId, instanceEvent.getInstanceId());
    InstanceData instanceData = query.get();
    if (null != instanceData) {
      UpdateOperations<InstanceData> updateOperations = hPersistence.createUpdateOperations(InstanceData.class);

      Instant instant = instanceEvent.getTimestamp();
      switch (instanceEvent.getType()) {
        case STOP:
          updateOperations.set(InstanceDataKeys.usageStopTime, instant);
          updateOperations.set(InstanceDataKeys.instanceState, InstanceState.STOPPED);
          break;
        case START:
          updateOperations.set(InstanceDataKeys.usageStartTime, instant);
          updateOperations.set(InstanceDataKeys.instanceState, InstanceState.RUNNING);
          break;
        default:
          break;
      }
      FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(true).returnNew(false);
      return hPersistence.upsert(query, updateOperations, findAndModifyOptions);
    } else {
      logger.warn("Instance Event received before info event {}", instanceEvent.getInstanceId());
    }
    return null;
  }

  @Override
  public InstanceData upsert(InstanceInfo instanceInfo) {
    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class)
                                    .filter(InstanceDataKeys.accountId, instanceInfo.getAccountId())
                                    .filter(InstanceDataKeys.clusterId, instanceInfo.getClusterId())
                                    .filter(InstanceDataKeys.instanceId, instanceInfo.getInstanceId());
    InstanceData instanceData = query.get();
    if (null == instanceData) {
      UpdateOperations<InstanceData> updateOperations =
          hPersistence.createUpdateOperations(InstanceData.class)
              .set(InstanceDataKeys.accountId, instanceInfo.getAccountId())
              .set(InstanceDataKeys.settingId, instanceInfo.getSettingId())
              .set(InstanceDataKeys.instanceId, instanceInfo.getInstanceId())
              .set(InstanceDataKeys.instanceName, instanceInfo.getInstanceName())
              .set(InstanceDataKeys.instanceType, instanceInfo.getInstanceType())
              .set(InstanceDataKeys.clusterId, instanceInfo.getClusterId())
              .set(InstanceDataKeys.clusterName, instanceInfo.getClusterName())
              .set(InstanceDataKeys.instanceState, instanceInfo.getInstanceState());

      if (!isNull(instanceInfo.getResource())) {
        updateOperations.set(InstanceDataKeys.totalResource, instanceInfo.getResource());
      }

      if (!isNull(instanceInfo.getLabels())) {
        updateOperations.set(InstanceDataKeys.labels, instanceInfo.getLabels());
      }

      if (instanceInfo.getMetaData() != null) {
        updateOperations.set(InstanceDataKeys.metaData, instanceInfo.getMetaData());
      }

      if (instanceInfo.getHarnessServiceInfo() != null) {
        updateOperations.set(InstanceDataKeys.harnessServiceInfo, instanceInfo.getHarnessServiceInfo());
      }

      FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(true).returnNew(true);

      return hPersistence.upsert(query, updateOperations, findAndModifyOptions);
    } else {
      logger.trace("Instance data found {} ", instanceData);
    }
    return instanceData;
  }

  @Override
  public boolean updateInstanceState(
      InstanceData instanceData, Instant instant, String instantField, InstanceState instanceState) {
    UpdateOperations<InstanceData> instanceDataUpdateOperations =
        hPersistence.createUpdateOperations(InstanceData.class)
            .set(instantField, instant)
            .set(InstanceDataKeys.instanceState, instanceState);

    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class)
                                    .filter(InstanceDataKeys.accountId, instanceData.getAccountId())
                                    .filter(InstanceDataKeys.clusterId, instanceData.getClusterId())
                                    .filter(InstanceDataKeys.instanceId, instanceData.getInstanceId());

    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(true).returnNew(false);
    return hPersistence.upsert(query, instanceDataUpdateOperations, findAndModifyOptions) != null;
  }

  @Override
  public InstanceData fetchActiveInstanceData(
      String accountId, String clusterId, String instanceId, List<InstanceState> instanceState) {
    return hPersistence.createQuery(InstanceData.class)
        .filter(InstanceDataKeys.accountId, accountId)
        .filter(InstanceDataKeys.clusterId, clusterId)
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

  @Override
  public InstanceData fetchInstanceDataWithName(
      String accountId, String clusterId, String instanceName, Long occurredAt) {
    return hPersistence.createQuery(InstanceData.class)
        .filter(InstanceDataKeys.accountId, accountId)
        .filter(InstanceDataKeys.clusterId, clusterId)
        .filter(InstanceDataKeys.instanceName, instanceName)
        .order(Sort.descending(InstanceDataKeys.usageStartTime))
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
        .filter(InstanceDataKeys.clusterId, clusterName)
        .field(InstanceDataKeys.instanceState)
        .in(instanceState)
        .field(InstanceDataKeys.usageStartTime)
        .lessThanOrEq(startTime)
        .asList();
  }
}
