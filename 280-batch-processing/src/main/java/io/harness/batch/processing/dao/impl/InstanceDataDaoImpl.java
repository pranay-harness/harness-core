package io.harness.batch.processing.dao.impl;

import static io.harness.ccm.commons.beans.InstanceType.K8S_PV;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.persistence.HQuery.excludeAuthorityCount;
import static io.harness.persistence.HQuery.excludeCount;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.events.timeseries.data.CostEventData;
import io.harness.batch.processing.events.timeseries.service.intfc.CostEventService;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.support.ActiveInstanceIterator;
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
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
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
    instanceData.setActiveInstanceIterator(stopTime);
    instanceData.setInstanceState(InstanceState.STOPPED);
    instanceData.setTtl(new Date(stopTime.plus(30, ChronoUnit.DAYS).toEpochMilli()));
    return hPersistence.save(instanceData) != null;
  }

  private Map<String, List<InstanceData>> getInstanceData(List<InstanceEvent> instanceEvents) {
    Set<String> instanceIds = instanceEvents.stream().map(InstanceEvent::getInstanceId).collect(Collectors.toSet());
    List<InstanceData> instanceData = fetchInstanceData(instanceIds);
    return instanceData.stream().collect(Collectors.groupingBy(InstanceData::getInstanceId));
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
  public void updateInstanceActiveIterationTime(InstanceData instanceData) {
    try {
      instanceData.setActiveInstanceIterator(
          ActiveInstanceIterator.getActiveInstanceIteratorFromStartTime(instanceData.getUsageStartTime()));
      hPersistence.save(instanceData);
    } catch (Exception ex) {
      log.info("Instance data {}", instanceData);
      log.error("Error is ", ex);
    }
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
  public List<InstanceData> fetchActivePVList(String accountId, Instant startTime, Instant endTime) {
    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class)
                                    .filter(InstanceDataKeys.accountId, accountId)
                                    .filter(InstanceDataKeys.instanceType, K8S_PV);

    query.and(query.or(query.and(query.criteria(InstanceDataKeys.usageStartTime).lessThanOrEq(startTime),
                           query.or(query.criteria(InstanceDataKeys.usageStopTime).doesNotExist(),
                               query.criteria(InstanceDataKeys.usageStopTime).greaterThan(startTime))),
        query.and(query.criteria(InstanceDataKeys.usageStartTime).greaterThanOrEq(startTime),
            query.criteria(InstanceDataKeys.usageStartTime).lessThan(endTime))));

    return query.asList();
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
        if (null == instanceData.getUsageStopTime()) {
          instanceIds.add(instanceData.getInstanceId());
        }
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

  @Override
  public List<InstanceData> getInstanceDataListsOfType(String accountId, int batchSize, Instant startTime,
      Instant endTime, Instant seekingDate, InstanceType instanceType) {
    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class, excludeCount)
                                    .filter(InstanceDataKeys.accountId, accountId)
                                    .filter(InstanceDataKeys.instanceType, instanceType)
                                    .order(InstanceDataKeys.usageStartTime);

    query.and(query.criteria(InstanceDataKeys.usageStartTime).greaterThanOrEq(seekingDate),
        query.criteria(InstanceDataKeys.usageStartTime).lessThanOrEq(endTime),
        query.or(query.criteria(InstanceDataKeys.usageStopTime).greaterThan(startTime),
            query.criteria(InstanceDataKeys.usageStopTime).doesNotExist()));
    return query.asList(new FindOptions().limit(batchSize));
  }

  @Override
  public List<InstanceData> getInstanceDataListsOtherThanPV(
      String accountId, int batchSize, Instant startTime, Instant endTime, Instant seekingDate) {
    Query<InstanceData> query = hPersistence.createQuery(InstanceData.class, excludeCount)
                                    .filter(InstanceDataKeys.accountId, accountId)
                                    .field(InstanceDataKeys.instanceType)
                                    .notEqual(K8S_PV)
                                    .order(InstanceDataKeys.usageStartTime);

    query.and(query.criteria(InstanceDataKeys.usageStartTime).greaterThanOrEq(seekingDate),
        query.criteria(InstanceDataKeys.usageStartTime).lessThanOrEq(endTime),
        query.or(query.criteria(InstanceDataKeys.usageStopTime).greaterThan(startTime),
            query.criteria(InstanceDataKeys.usageStopTime).doesNotExist()));
    return query.asList(new FindOptions().limit(batchSize));
  }

  // TODO(utsav): refactor; add "query.and(..." from above three to one here
  private void enforceActiveInstances() {}
}
