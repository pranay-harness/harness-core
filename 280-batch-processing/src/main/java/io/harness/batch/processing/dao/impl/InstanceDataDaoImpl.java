package io.harness.batch.processing.dao.impl;

import static io.harness.persistence.HPersistence.upsertReturnNewOptions;
import static io.harness.persistence.HPersistence.upsertReturnOldOptions;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.persistence.HQuery.excludeAuthorityCount;
import static io.harness.persistence.HQuery.excludeCount;

import static java.util.Objects.isNull;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.events.timeseries.data.CostEventData;
import io.harness.batch.processing.events.timeseries.service.intfc.CostEventService;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.tasklet.util.InstanceMetaDataUtils;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.ccm.commons.beans.InstanceState;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.entities.InstanceData;
import io.harness.ccm.commons.entities.InstanceData.InstanceDataKeys;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class InstanceDataDaoImpl implements InstanceDataDao {
  @Autowired @Inject private HPersistence hPersistence;
  @Autowired private CostEventService costEventService;

  @Override
  public boolean create(InstanceData instanceData) {
    return hPersistence.save(instanceData) != null;
  }

  @Override
  public boolean updateInstanceStopTime(InstanceData instanceData, Instant stopTime) {
    instanceData.setUsageStopTime(stopTime);
    instanceData.setInstanceState(InstanceState.STOPPED);
    return hPersistence.save(instanceData) != null;
  }

  @Override
  public InstanceData upsert(InstanceEvent instanceEvent) {
    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class)
                                    .filter(InstanceDataKeys.accountId, instanceEvent.getAccountId())
                                    .filter(InstanceDataKeys.clusterId, instanceEvent.getClusterId())
                                    .filter(InstanceDataKeys.instanceId, instanceEvent.getInstanceId());
    InstanceData instanceData = query.get();
    return updateInstanceLifecycleData(instanceEvent, instanceData);
  }

  private InstanceData updateInstanceLifecycleData(InstanceEvent instanceEvent, InstanceData instanceData) {
    if (null != instanceData) {
      UpdateOperations<InstanceData> updateOperations = hPersistence.createUpdateOperations(InstanceData.class);
      Instant instant = instanceEvent.getTimestamp();
      boolean updateRequired = false;
      switch (instanceEvent.getType()) {
        case STOP:
          if (null == instanceData.getUsageStopTime()) {
            updateOperations.set(InstanceDataKeys.usageStopTime, instant);
            updateOperations.set(InstanceDataKeys.instanceState, InstanceState.STOPPED);
            updateRequired = true;
          }
          break;
        case START:
          if (null == instanceData.getUsageStartTime()) {
            updateOperations.set(InstanceDataKeys.usageStartTime, instant);
            updateOperations.set(InstanceDataKeys.instanceState, InstanceState.RUNNING);
            updateRequired = true;
          }
          break;
        default:
          break;
      }
      if (updateRequired) {
        Query<InstanceData> query = hPersistence.createQuery(InstanceData.class)
                                        .filter(InstanceDataKeys.accountId, instanceEvent.getAccountId())
                                        .filter(InstanceDataKeys.clusterId, instanceEvent.getClusterId())
                                        .filter(InstanceDataKeys.instanceId, instanceEvent.getInstanceId());
        return hPersistence.upsert(query, updateOperations, upsertReturnOldOptions);
      } else {
        return instanceData;
      }
    } else {
      log.debug("Instance Event received before info event {}", instanceEvent.getInstanceId());
    }
    return null;
  }

  @Override
  public void upsert(List<InstanceEvent> instanceEvents) {
    Map<String, List<InstanceData>> instanceDataMap = getInstanceData(instanceEvents);
    instanceEvents.forEach(instanceEvent -> {
      if (null != instanceDataMap.get(instanceEvent.getInstanceId())) {
        List<InstanceData> instanceDataList = instanceDataMap.get(instanceEvent.getInstanceId());
        instanceDataList.stream()
            .filter(instanceData -> instanceData.getClusterId().equals(instanceEvent.getClusterId()))
            .forEach(instanceData -> updateInstanceLifecycleData(instanceEvent, instanceData));
      } else {
        updateInstanceLifecycleData(instanceEvent, null);
      }
    });
  }

  private Map<String, List<InstanceData>> getInstanceData(List<InstanceEvent> instanceEvents) {
    Set<String> instanceIds = instanceEvents.stream().map(InstanceEvent::getInstanceId).collect(Collectors.toSet());
    List<InstanceData> instanceData = fetchInstanceData(instanceIds);
    return instanceData.stream().collect(Collectors.groupingBy(InstanceData::getInstanceId));
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
              .set(InstanceDataKeys.instanceState, instanceInfo.getInstanceState())
              .set(InstanceDataKeys.usageStartTime, instanceInfo.getUsageStartTime());

      if (!isNull(instanceInfo.getResource())) {
        updateOperations.set(InstanceDataKeys.totalResource, instanceInfo.getResource());
      }

      if (!isNull(instanceInfo.getCloudProviderInstanceId())) {
        updateOperations.set(InstanceDataKeys.cloudProviderInstanceId, instanceInfo.getCloudProviderInstanceId());
      }

      if (!isNull(instanceInfo.getResourceLimit())) {
        updateOperations.set(InstanceDataKeys.limitResource, instanceInfo.getResourceLimit());
      }

      if (!isNull(instanceInfo.getAllocatableResource())) {
        updateOperations.set(InstanceDataKeys.allocatableResource, instanceInfo.getAllocatableResource());
      }

      if (!isNull(instanceInfo.getStorageResource())) {
        updateOperations.set(InstanceDataKeys.storageResource, instanceInfo.getStorageResource());
      }

      if (!isNull(instanceInfo.getLabels())) {
        updateOperations.set(InstanceDataKeys.labels, instanceInfo.getLabels());
      }

      if (!isNull(instanceInfo.getNamespaceLabels()) && !instanceInfo.getNamespaceLabels().isEmpty()) {
        updateOperations.set(InstanceDataKeys.namespaceLabels, instanceInfo.getNamespaceLabels());
      }

      if (instanceInfo.getMetaData() != null) {
        updateOperations.set(InstanceDataKeys.metaData, instanceInfo.getMetaData());
      }

      if (instanceInfo.getHarnessServiceInfo() != null) {
        updateOperations.set(InstanceDataKeys.harnessServiceInfo, instanceInfo.getHarnessServiceInfo());
      }

      InstanceData savedInstanceData = hPersistence.upsert(query, updateOperations, upsertReturnNewOptions);

      if (savedInstanceData.getHarnessServiceInfo() != null
          && !InstanceMetaDataConstants.SLOW_ACCOUNT.equals(instanceInfo.getAccountId())) {
        try {
          updateDeploymentEvent(savedInstanceData);
        } catch (Exception e) {
          log.error("Exception while updating deployment event ", e);
        }
      }
      return savedInstanceData;

    } else {
      log.trace("Instance data found {} ", instanceData);
      Map<String, String> oldInstanceMetaData = instanceData.getMetaData();
      Map<String, String> newInstanceMetaData = instanceInfo.getMetaData();

      if (false
          && InstanceMetaDataUtils.carryUpdatedMapKeyFromTo(newInstanceMetaData,
              oldInstanceMetaData)) { // carry updated (key,value) from newInstanceMetaData to oldInstanceMetaData
        UpdateOperations<InstanceData> updateOperations = hPersistence.createUpdateOperations(InstanceData.class);
        updateOperations.set(InstanceDataKeys.metaData, oldInstanceMetaData);
        return hPersistence.upsert(query, updateOperations, upsertReturnNewOptions);
      }
    }
    return instanceData;
  }

  @Override
  public InstanceData fetchInstanceData(String instanceId) {
    return hPersistence.createQuery(InstanceData.class, excludeAuthority)
        .filter(InstanceDataKeys.instanceId, instanceId)
        .get();
  }

  @Override
  public List<InstanceData> fetchInstanceData(Set<String> instanceIds) {
    if (instanceIds.isEmpty()) {
      return Collections.emptyList();
    } else {
      return hPersistence.createQuery(InstanceData.class, excludeAuthorityCount)
          .field(InstanceDataKeys.instanceId)
          .in(instanceIds)
          .asList();
    }
  }

  private void updateDeploymentEvent(InstanceData instanceData) {
    CostEventData costEventData = CostEventData.builder()
                                      .settingId(instanceData.getSettingId())
                                      .accountId(instanceData.getAccountId())
                                      .clusterId(instanceData.getClusterId())
                                      .clusterType(InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
                                          InstanceMetaDataConstants.CLUSTER_TYPE, instanceData))
                                      .cloudProvider(InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
                                          InstanceMetaDataConstants.CLOUD_PROVIDER, instanceData))
                                      .namespace(InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
                                          InstanceMetaDataConstants.NAMESPACE, instanceData))
                                      .workloadName(InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
                                          InstanceMetaDataConstants.WORKLOAD_NAME, instanceData))
                                      .workloadType(InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
                                          InstanceMetaDataConstants.WORKLOAD_TYPE, instanceData))
                                      .deploymentId(instanceData.getHarnessServiceInfo().getDeploymentSummaryId())
                                      .build();
    costEventService.updateDeploymentEvent(costEventData);
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

    return hPersistence.upsert(query, instanceDataUpdateOperations, upsertReturnOldOptions) != null;
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
  public InstanceData fetchInstanceData(String accountId, String clusterId, String instanceId) {
    return hPersistence.createQuery(InstanceData.class)
        .filter(InstanceDataKeys.accountId, accountId)
        .filter(InstanceDataKeys.clusterId, clusterId)
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

  @Override
  public Set<String> fetchClusterActiveInstanceIds(
      String accountId, String clusterName, List<InstanceState> instanceState, Instant startTime) {
    Set<String> instanceIds = new HashSet<>();
    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class)
                                    .filter(InstanceDataKeys.accountId, accountId)
                                    .filter(InstanceDataKeys.clusterId, clusterName)
                                    .field(InstanceDataKeys.instanceState)
                                    .in(instanceState)
                                    .field(InstanceDataKeys.usageStartTime)
                                    .lessThanOrEq(startTime);
    try (HIterator<InstanceData> instanceItr = new HIterator<>(query.fetch())) {
      for (InstanceData instanceData : instanceItr) {
        instanceIds.add(instanceData.getInstanceId());
      }
    }
    return instanceIds;
  }

  @Override
  public List<InstanceData> fetchClusterActiveInstanceData(
      String accountId, String clusterName, List<InstanceType> instanceTypes, InstanceState instanceState) {
    return hPersistence.createQuery(InstanceData.class)
        .filter(InstanceDataKeys.accountId, accountId)
        .filter(InstanceDataKeys.clusterId, clusterName)
        .field(InstanceDataKeys.instanceType)
        .in(instanceTypes)
        .filter(InstanceDataKeys.instanceState, instanceState)
        .asList();
  }

  @Override
  public InstanceData getActiveInstance(
      String accountId, Instant startTime, Instant endTime, CloudProvider cloudProvider) {
    Query<InstanceData> query =
        hPersistence.createQuery(InstanceData.class).filter(InstanceDataKeys.accountId, accountId);
    query.and(query.or(query.and(query.criteria(InstanceDataKeys.usageStartTime).lessThanOrEq(startTime),
                           query.or(query.criteria(InstanceDataKeys.usageStopTime).doesNotExist(),
                               query.criteria(InstanceDataKeys.usageStopTime).greaterThan(startTime))),
        query.and(query.criteria(InstanceDataKeys.usageStartTime).greaterThanOrEq(startTime),
            query.criteria(InstanceDataKeys.usageStartTime).lessThan(endTime))));
    query.filter(InstanceDataKeys.CLOUD_PROVIDER, cloudProvider);
    return query.get();
  }

  @Override
  public InstanceData getK8sPodInstance(String accountId, String clusterId, String namespace, String podName) {
    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class)
                                    .field(InstanceDataKeys.accountId)
                                    .equal(accountId)
                                    .field(InstanceDataKeys.clusterId)
                                    .equal(clusterId)
                                    .field(InstanceDataKeys.instanceName)
                                    .equal(podName)
                                    .field(InstanceDataKeys.metaData + "." + InstanceMetaDataConstants.NAMESPACE)
                                    .equal(namespace);
    return query.get();
  }

  @Override
  public List<InstanceData> fetchInstanceDataForGivenInstances(
      String accountId, String clusterId, List<String> instanceIds) {
    return hPersistence.createQuery(InstanceData.class)
        .filter(InstanceDataKeys.accountId, accountId)
        .filter(InstanceDataKeys.clusterId, clusterId)
        .field(InstanceDataKeys.instanceId)
        .in(instanceIds)
        .asList();
  }

  @Override
  public List<InstanceData> getInstanceDataLists(
      String accountId, int batchSize, Instant startTime, Instant endTime, Instant seekingDate) {
    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class, excludeCount)
                                    .filter(InstanceDataKeys.accountId, accountId)
                                    .order(InstanceDataKeys.usageStartTime);

    query.and(query.criteria(InstanceDataKeys.usageStartTime).greaterThanOrEq(seekingDate),
        query.criteria(InstanceDataKeys.usageStartTime).lessThanOrEq(endTime),
        query.or(query.criteria(InstanceDataKeys.usageStopTime).greaterThan(startTime),
            query.criteria(InstanceDataKeys.usageStopTime).doesNotExist()));
    return query.asList(new FindOptions().limit(batchSize));
  }
}
