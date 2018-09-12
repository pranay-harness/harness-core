package software.wings.service.impl.instance;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.ManualSyncJob;
import software.wings.beans.infrastructure.instance.SyncStatus;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.beans.infrastructure.instance.key.InstanceKey;
import software.wings.beans.infrastructure.instance.key.PcfInstanceKey;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.utils.Validator;

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
public class InstanceServiceImpl implements InstanceService {
  private static final Logger logger = LoggerFactory.getLogger(InstanceServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;

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

    Instance currentInstance = get(instance.getUuid());
    Validator.nullCheck("Instance", currentInstance);

    String key = wingsPersistence.save(instance);
    Instance updatedInstance = wingsPersistence.get(Instance.class, instance.getAppId(), key);
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
  public Instance get(String instanceId) {
    return wingsPersistence.get(Instance.class, instanceId);
  }

  @Override
  public Instance saveOrUpdate(@Valid Instance instance) {
    Query<Instance> query = wingsPersistence.createAuthorizedQuery(Instance.class);
    InstanceKey instanceKey = addInstanceKeyFilterToQuery(query, instance);
    synchronized (instanceKey) {
      Instance existingInstance = query.get();
      if (existingInstance == null) {
        return save(instance);
      } else {
        instance.setUuid(existingInstance.getUuid());
        String uuid = wingsPersistence.merge(instance);
        return wingsPersistence.get(Instance.class, uuid);
      }
    }
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
    } else {
      String msg = "Either host or container or pcf instance key needs to be set";
      logger.error(msg);
      throw new WingsException(msg);
    }
  }

  private void pruneByEntity(String fieldName, String value) {
    Query<Instance> query = wingsPersistence.createAuthorizedQuery(Instance.class);
    query.filter(fieldName, value);
    wingsPersistence.delete(query);
  }

  private void pruneByEntity(Map<String, String> inputs) {
    Query<Instance> query = wingsPersistence.createAuthorizedQuery(Instance.class);
    inputs.forEach((key, value) -> query.filter(key, value));
    wingsPersistence.delete(query);
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
    HashMap<String, String> map = Maps.newHashMap();
    map.put("appId", appId);
    map.put("envId", envId);
    pruneByEntity(map);
  }

  @Override
  public void pruneByInfrastructureMapping(String appId, String infraMappingId) {
    HashMap<String, String> map = Maps.newHashMap();
    map.put("appId", appId);
    map.put("infraMappingId", infraMappingId);
    pruneByEntity(map);
  }

  @Override
  public void pruneByService(String appId, String serviceId) {
    HashMap<String, String> map = Maps.newHashMap();
    map.put("appId", appId);
    map.put("serviceId", serviceId);
    pruneByEntity(map);
  }

  @Override
  public boolean delete(Set<String> instanceIdSet) {
    Query<Instance> query = wingsPersistence.createAuthorizedQuery(Instance.class);
    query.field("_id").in(instanceIdSet);
    return wingsPersistence.delete(query);
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
    PageRequest<SyncStatus> pageRequest = aPageRequest()
                                              .addFilter("appId", EQ, appId)
                                              .addFilter("serviceId", EQ, serviceId)
                                              .addFilter("envId", EQ, envId)
                                              .addFilter("infraMappingId", EQ, infraMappingId)
                                              .build();
    return wingsPersistence.get(SyncStatus.class, pageRequest);
  }

  @Override
  public void updateSyncFailure(String appId, String serviceId, String envId, String infraMappingId,
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
  public List<Boolean> getManualSyncJobsStatus(Set<String> manualJobIdSet) {
    List<Key<ManualSyncJob>> keyList =
        wingsPersistence.createQuery(ManualSyncJob.class).field("_id").in(manualJobIdSet).asKeyList();
    Set<Object> jobIdSetInDB = keyList.stream().map(key -> key.getId()).collect(Collectors.toSet());
    List<Boolean> result = Lists.newArrayList();
    manualJobIdSet.forEach(jobId -> result.add(!jobIdSetInDB.contains(jobId)));
    return result;
  }
}
