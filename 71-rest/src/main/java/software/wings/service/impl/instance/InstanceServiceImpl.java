package software.wings.service.impl.instance;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.validation.Validator.nullCheck;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.data.structure.UUIDGenerator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.persistence.HIterator;
import io.harness.queue.QueuePublisher;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.api.InstanceEvent;
import software.wings.beans.Account;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceKeys;
import software.wings.beans.infrastructure.instance.ManualSyncJob;
import software.wings.beans.infrastructure.instance.SyncStatus;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.beans.infrastructure.instance.key.InstanceKey;
import software.wings.beans.infrastructure.instance.key.PcfInstanceKey;
import software.wings.beans.infrastructure.instance.key.PodInstanceKey;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.instance.InstanceService;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;

/**
 * @author rktummala on 8/13/17
 */
@Singleton
@Slf4j
public class InstanceServiceImpl implements InstanceService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private QueuePublisher<InstanceEvent> eventQueue;

  @Override
  public Instance save(Instance instance) {
    if (logger.isDebugEnabled()) {
      logger.debug("Begin - Instance save called for uuid:" + instance.getUuid()
          + " and infraMappingId:" + instance.getInfraMappingId());
    }
    if (!appService.exist(instance.getAppId())) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "App does not exist: " + instance.getAppId());
    }

    Instance currentInstance = get(instance.getUuid(), false);
    nullCheck("Instance", currentInstance);

    String key = wingsPersistence.save(instance);
    Instance updatedInstance = wingsPersistence.getWithAppId(Instance.class, instance.getAppId(), key);
    if (logger.isDebugEnabled()) {
      logger.debug("End - Instance save called for uuid:" + instance.getUuid()
          + " and infraMappingId:" + instance.getInfraMappingId());
    }

    return updatedInstance;
  }

  @Override
  public List<Instance> saveOrUpdate(List<Instance> instances) {
    List<Instance> updatedInstanceList = Lists.newArrayList();
    for (Instance instance : instances) {
      Instance updatedInstance = saveOrUpdate(instance);
      if (updatedInstance != null) {
        updatedInstanceList.add(updatedInstance);
      }
    }
    return updatedInstanceList;
  }

  @Override
  public Instance get(String instanceId, boolean includeDeleted) {
    Instance instance = wingsPersistence.get(Instance.class, instanceId);
    if (instance != null && !includeDeleted && instance.isDeleted()) {
      return null;
    }
    return instance;
  }

  @Override
  public Instance saveOrUpdate(@Valid Instance instance) {
    Query<Instance> query = wingsPersistence.createQuery(Instance.class);
    query.filter("accountId", instance.getAccountId());
    query.filter("appId", instance.getAppId());
    query.filter("isDeleted", false);
    InstanceKey instanceKey = addInstanceKeyFilterToQuery(query, instance);

    try (AcquiredLock acquiredLock =
             persistentLocker.waitToAcquireLock(instanceKey.toString(), Duration.ofMinutes(1), Duration.ofMinutes(2))) {
      if (acquiredLock == null) {
        String msg = "Unable to acquire lock while trying save or update instance with key " + instanceKey.toString();
        logger.warn(msg);
        throw new WingsException(msg);
      }

      Instance existingInstance = query.get();
      if (existingInstance == null) {
        return save(instance);
      } else {
        instance.setUuid(UUIDGenerator.generateUuid());
        return update(instance, existingInstance.getUuid());
      }
    }
  }

  @Override
  public Instance update(Instance instance, String oldInstanceId) {
    delete(Sets.newHashSet(oldInstanceId));

    // since this is a new version, we have to make sure that the deletedAt of old version and
    // createdAt of new version are off by at least 1 milliseconds.
    // Otherwise, if the stats collection happens in that exact millisecond,
    // they will see twice the instance count for the ones that were in the version update processing.
    instance.setCreatedAt(System.currentTimeMillis() + 1);
    String uuid = wingsPersistence.save(instance);
    return wingsPersistence.get(Instance.class, uuid);
  }

  private InstanceKey addInstanceKeyFilterToQuery(Query<Instance> query, Instance instance) {
    if (instance.getHostInstanceKey() != null) {
      HostInstanceKey hostInstanceKey = instance.getHostInstanceKey();
      query.filter("hostInstanceKey.hostName", hostInstanceKey.getHostName())
          .filter("hostInstanceKey.infraMappingId", hostInstanceKey.getInfraMappingId());
      return hostInstanceKey;
    } else if (instance.getContainerInstanceKey() != null) {
      ContainerInstanceKey containerInstanceKey = instance.getContainerInstanceKey();
      query.filter("containerInstanceKey.containerId", containerInstanceKey.getContainerId());
      return containerInstanceKey;
    } else if (instance.getPcfInstanceKey() != null) {
      PcfInstanceKey pcfInstanceKey = instance.getPcfInstanceKey();
      query.filter("pcfInstanceKey.id", pcfInstanceKey.getId());
      return pcfInstanceKey;
    } else if (instance.getPodInstanceKey() != null) {
      PodInstanceKey podInstanceKey = instance.getPodInstanceKey();
      query.filter("podInstanceKey.podName", podInstanceKey.getPodName())
          .filter("podInstanceKey.namespace", podInstanceKey.getNamespace());
      return podInstanceKey;
    } else {
      String msg = "Either host or container or pcf instance key needs to be set";
      logger.error(msg);
      throw new WingsException(msg);
    }
  }

  private void pruneByEntity(String fieldName, String value) {
    Query<Instance> query = wingsPersistence.createQuery(Instance.class);
    query.filter(fieldName, value);
    delete(query);

    if ("appId".equals(fieldName)) {
      Query<SyncStatus> syncStatusQuery = wingsPersistence.createQuery(SyncStatus.class);
      syncStatusQuery.filter(fieldName, value);
      wingsPersistence.delete(syncStatusQuery);
    }
  }

  private void pruneByEntity(Map<String, String> inputs) {
    Query<Instance> query = wingsPersistence.createQuery(Instance.class);
    inputs.forEach((key, value) -> query.filter(key, value));
    delete(query);

    Query<SyncStatus> syncStatusQuery = wingsPersistence.createQuery(SyncStatus.class);
    inputs.forEach(syncStatusQuery::filter);
    wingsPersistence.delete(syncStatusQuery);
  }

  @Override
  public void pruneByApplication(String appId) {
    pruneByEntity("appId", appId);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    pruneByEntity("accountId", accountId);
  }

  @Override
  public void pruneByEnvironment(String appId, String envId) {
    HashMap<String, String> map = new HashMap<>();
    map.put("appId", appId);
    map.put("envId", envId);
    pruneByEntity(map);
  }

  @Override
  public void pruneByInfrastructureMapping(String appId, String infraMappingId) {
    HashMap<String, String> map = new HashMap<>();
    map.put("appId", appId);
    map.put("infraMappingId", infraMappingId);
    pruneByEntity(map);
  }

  @Override
  public void pruneByService(String appId, String serviceId) {
    HashMap<String, String> map = new HashMap<>();
    map.put("appId", appId);
    map.put("serviceId", serviceId);
    pruneByEntity(map);
  }

  @Override
  public boolean delete(Set<String> instanceIdSet) {
    Query<Instance> query = wingsPersistence.createQuery(Instance.class);
    query.field("_id").in(instanceIdSet);
    return delete(query);
  }

  private boolean delete(Query<Instance> query) {
    long currentTimeMillis = System.currentTimeMillis();
    UpdateOperations<Instance> updateOperations = wingsPersistence.createUpdateOperations(Instance.class);
    setUnset(updateOperations, "deletedAt", currentTimeMillis);
    setUnset(updateOperations, "isDeleted", true);
    wingsPersistence.update(query, updateOperations);
    return true;
  }

  @Override
  public boolean purgeDeletedUpTo(Instant timestamp) {
    try (HIterator<Account> accounts =
             new HIterator<>(wingsPersistence.createQuery(Account.class).project(Account.ID_KEY, true).fetch())) {
      while (accounts.hasNext()) {
        final Account account = accounts.next();
        Query<Instance> query = wingsPersistence.createQuery(Instance.class)
                                    .filter(InstanceKeys.accountId, account.getUuid())
                                    .filter(InstanceKeys.isDeleted, true)
                                    .field(InstanceKeys.deletedAt)
                                    .lessThan(timestamp.toEpochMilli());
        wingsPersistence.delete(query);
      }
    }
    return true;
  }

  @Override
  public List<ContainerDeploymentInfo> getContainerDeploymentInfoList(String containerSvcNameNoRevision, String appId) {
    PageRequest<ContainerDeploymentInfo> pageRequest =
        aPageRequest()
            .addFilter("containerSvcNameNoRevision", EQ, containerSvcNameNoRevision)
            .addFilter("appId", EQ, appId)
            .addOrder("containerSvcName", OrderType.ASC)
            .build();
    PageResponse<ContainerDeploymentInfo> response = wingsPersistence.query(ContainerDeploymentInfo.class, pageRequest);
    return response.getResponse();
  }

  @Override
  public PageResponse<Instance> list(PageRequest<Instance> pageRequest) {
    pageRequest.addFilter("isDeleted", Operator.EQ, false);
    return wingsPersistence.query(Instance.class, pageRequest);
  }

  @Override
  public void updateSyncSuccess(
      String appId, String serviceId, String envId, String infraMappingId, String infraMappingName, long timestamp) {
    SyncStatus syncStatus = getSyncStatus(appId, serviceId, envId, infraMappingId);
    if (syncStatus == null) {
      syncStatus = SyncStatus.builder()
                       .appId(appId)
                       .envId(envId)
                       .serviceId(serviceId)
                       .infraMappingId(infraMappingId)
                       .infraMappingName(infraMappingName)
                       .lastSyncedAt(timestamp)
                       .lastSuccessfullySyncedAt(timestamp)
                       .syncFailureReason(null)
                       .build();
    } else {
      syncStatus.setSyncFailureReason(null);
      syncStatus.setLastSuccessfullySyncedAt(timestamp);
      syncStatus.setLastSyncedAt(timestamp);
    }

    wingsPersistence.save(syncStatus);
  }

  private SyncStatus getSyncStatus(String appId, String serviceId, String envId, String infraMappingId) {
    return wingsPersistence.createQuery(SyncStatus.class)
        .filter(SyncStatus.APP_ID_KEY, appId)
        .filter(SyncStatus.SERVICE_ID_KEY, serviceId)
        .filter(SyncStatus.ENV_ID_KEY, envId)
        .filter(SyncStatus.INFRA_MAPPING_ID_KEY, infraMappingId)
        .get();
  }

  @Override
  public boolean handleSyncFailure(String appId, String serviceId, String envId, String infraMappingId,
      String infraMappingName, long timestamp, String errorMsg) {
    SyncStatus syncStatus = getSyncStatus(appId, serviceId, envId, infraMappingId);
    if (syncStatus != null) {
      if ((timestamp - syncStatus.getLastSuccessfullySyncedAt()) >= Duration.ofDays(7).toMillis()) {
        logger.info("Deleting the instances since sync has been failing for more than a week for infraMappingId: {}",
            infraMappingId);
        wingsPersistence.delete(SyncStatus.class, syncStatus.getUuid());
        pruneByInfrastructureMapping(appId, infraMappingId);
        return false;
      }
    }

    updateSyncFailure(appId, serviceId, envId, infraMappingId, infraMappingName, timestamp, errorMsg);
    return true;
  }

  private void updateSyncFailure(String appId, String serviceId, String envId, String infraMappingId,
      String infraMappingName, long timestamp, String errorMsg) {
    SyncStatus syncStatus = getSyncStatus(appId, serviceId, envId, infraMappingId);
    if (syncStatus == null) {
      syncStatus = SyncStatus.builder()
                       .appId(appId)
                       .envId(envId)
                       .serviceId(serviceId)
                       .infraMappingId(infraMappingId)
                       .infraMappingName(infraMappingName)
                       .lastSyncedAt(timestamp)
                       .syncFailureReason(errorMsg)
                       .build();
    } else {
      syncStatus.setSyncFailureReason(errorMsg);
      syncStatus.setLastSyncedAt(timestamp);
    }
    wingsPersistence.save(syncStatus);
  }

  @Override
  public void saveManualSyncJob(ManualSyncJob manualSyncJob) {
    wingsPersistence.save(manualSyncJob);
  }

  @Override
  public void deleteManualSyncJob(String appId, String manualSyncJobId) {
    wingsPersistence.delete(ManualSyncJob.class, appId, manualSyncJobId);
  }

  @Override
  public List<SyncStatus> getSyncStatus(String appId, String serviceId, String envId) {
    PageRequest<SyncStatus> pageRequest = aPageRequest()
                                              .addFilter("appId", EQ, appId)
                                              .addFilter("serviceId", EQ, serviceId)
                                              .addFilter("envId", EQ, envId)
                                              .build();
    PageResponse<SyncStatus> response = wingsPersistence.query(SyncStatus.class, pageRequest);
    return response.getResponse();
  }

  @Override
  public List<Boolean> getManualSyncJobsStatus(String accountId, Set<String> manualJobIdSet) {
    List<Key<ManualSyncJob>> keyList = wingsPersistence.createQuery(ManualSyncJob.class)
                                           .filter(ManualSyncJob.ACCOUNT_ID_KEY, accountId)
                                           .field("_id")
                                           .in(manualJobIdSet)
                                           .asKeyList();
    Set<Object> jobIdSetInDB = keyList.stream().map(Key::getId).collect(Collectors.toSet());
    List<Boolean> result = Lists.newArrayList();
    manualJobIdSet.forEach(jobId -> result.add(!jobIdSetInDB.contains(jobId)));
    return result;
  }

  public List<Instance> getInstancesForAppAndInframapping(String appId, String infraMappingId) {
    PageRequest<Instance> pageRequest = new PageRequest<>();
    pageRequest.addFilter("infraMappingId", Operator.EQ, infraMappingId);
    pageRequest.addFilter("appId", Operator.EQ, appId);
    PageResponse<Instance> pageResponse = list(pageRequest);
    return pageResponse.getResponse();
  }

  @Override
  public long getInstanceCount(String appId, String infraMappingId) {
    return wingsPersistence.createQuery(Instance.class)
        .filter(InstanceKeys.appId, appId)
        .filter(InstanceKeys.infraMappingId, infraMappingId)
        .filter(InstanceKeys.isDeleted, Boolean.FALSE)
        .count();
  }
}
