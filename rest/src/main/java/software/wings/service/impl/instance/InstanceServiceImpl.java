package software.wings.service.impl.instance;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static software.wings.beans.infrastructure.instance.InstanceType.ECS_CONTAINER_INSTANCE;
import static software.wings.beans.infrastructure.instance.InstanceType.KUBERNETES_CONTAINER_INSTANCE;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.beans.infrastructure.instance.key.InstanceKey;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.utils.Validator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;

/**
 * @author rktummala on 8/13/17
 */
@Singleton
public class InstanceServiceImpl implements InstanceService {
  private static final Logger logger = LoggerFactory.getLogger(InstanceServiceImpl.class);
  // We want to maintain only 10 revisions of each container service family.
  private int MAX_CONTAINER_SERVICE_COUNT = 10;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private InstanceUtil instanceUtil;

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
  public List<Instance> save(List<Instance> instances) {
    List<Instance> createdInstanceList = Lists.newArrayList();
    for (Instance instance : instances) {
      Instance createdInstance = save(instance);
      if (createdInstance != null) {
        createdInstanceList.add(createdInstance);
      }
    }
    return createdInstanceList;
  }

  @Override
  public List<Instance> update(List<Instance> instances) {
    List<Instance> updatedInstanceList = Lists.newArrayList();
    for (Instance instance : instances) {
      Instance updatedInstance;
      try {
        updatedInstance = update(instance);
        if (updatedInstance != null) {
          updatedInstanceList.add(updatedInstance);
        }
      } catch (Exception e) {
        logger.error("Update failed for instance entity:" + instance.getUuid());
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

  @Override
  public Instance update(@Valid Instance instance) throws Exception {
    Query<Instance> query = wingsPersistence.createAuthorizedQuery(Instance.class);
    query.field("_id").equal(instance.getUuid());
    Instance existingInstance = query.get();
    if (existingInstance == null) {
      throw new Exception("No entity exists with the id:" + instance.getUuid());
    } else {
      instance.setUuid(existingInstance.getUuid());
      String uuid = wingsPersistence.merge(instance);
      return wingsPersistence.get(Instance.class, uuid);
    }
  }

  private InstanceKey addInstanceKeyFilterToQuery(Query<Instance> query, Instance instance) {
    if (instance.getHostInstanceKey() != null) {
      HostInstanceKey hostInstanceKey = instance.getHostInstanceKey();
      query.field("hostInstanceKey.hostName")
          .equal(hostInstanceKey.getHostName())
          .field("hostInstanceKey.infraMappingId")
          .equal(hostInstanceKey.getInfraMappingId());
      return hostInstanceKey;
    } else if (instance.getContainerInstanceKey() != null) {
      ContainerInstanceKey containerInstanceKey = instance.getContainerInstanceKey();
      query.field("containerInstanceKey.containerId").equal(containerInstanceKey.getContainerId());
      return containerInstanceKey;
    } else {
      String msg = "Either host or container instance key needs to be set";
      logger.error(msg);
      throw new WingsException(msg);
    }
  }

  @Override
  public boolean delete(String instanceId) {
    Instance instance = get(instanceId);
    if (instance == null) {
      return true;
    }

    return wingsPersistence.delete(instance);
  }

  @Override
  public void pruneByApplication(String appId) {
    Query<Instance> query = wingsPersistence.createAuthorizedQuery(Instance.class);
    query.field("appId").equal(appId);
    wingsPersistence.delete(query);
  }

  @Override
  public boolean delete(Set<String> instanceIdSet) {
    Query<Instance> query = wingsPersistence.createAuthorizedQuery(Instance.class);
    query.field("_id").in(instanceIdSet);
    return wingsPersistence.delete(query);
  }

  @Override
  public void saveOrUpdateContainerInstances(
      InstanceType instanceType, String containerSvcNameNoRevision, List<Instance> instanceList, String appId) {
    Validator.notNullCheck("InstanceList", instanceList);

    Map<InstanceKey, Instance> currentInstanceMap =
        getCurrentInstancesInDB(instanceType, containerSvcNameNoRevision, appId);

    List<Instance> newInstanceList = Lists.newArrayList();
    List<Instance> updateInstanceList = Lists.newArrayList();

    for (Instance instance : instanceList) {
      Instance existingInstance = currentInstanceMap.remove(instance.getContainerInstanceKey());
      if (existingInstance == null) {
        newInstanceList.add(instance);
      } else if (shouldUpdateInstance(existingInstance, (ContainerInfo) instance.getInstanceInfo())) {
        updateInstanceList.add(instance);
      }
    }

    save(newInstanceList);
    update(updateInstanceList);
    Set<String> staleIds = currentInstanceMap.values().stream().map(Instance::getUuid).collect(toSet());
    delete(staleIds);
  }

  @Override
  public void saveOrUpdateContainerDeploymentInfo(String containerSvcNameNoRevision,
      Collection<ContainerDeploymentInfo> containerDeploymentInfoCollection, String appId, InstanceType instanceType,
      long syncTimestamp) {
    List<String> containerServiceNamesInDB = getContainerServiceNames(containerSvcNameNoRevision, appId);
    List<ContainerDeploymentInfo> newList = Lists.newArrayList();
    List<String> updateList = Lists.newArrayList();
    Set<String> deleteSet = Sets.newHashSet();
    NavigableSet<String> treeSet = Sets.newTreeSet(containerServiceNamesInDB);
    synchronized (containerSvcNameNoRevision) {
      for (ContainerDeploymentInfo containerDeploymentInfo : containerDeploymentInfoCollection) {
        if (!treeSet.contains(containerDeploymentInfo.getContainerSvcName())) {
          newList.add(containerDeploymentInfo);
        } else {
          updateList.add(containerDeploymentInfo.getContainerSvcName());
        }
      }

      int numOfEntriesToBeOverwritten = treeSet.size() + newList.size() - MAX_CONTAINER_SERVICE_COUNT;

      while (numOfEntriesToBeOverwritten > 0) {
        deleteSet.add(treeSet.pollFirst());
        numOfEntriesToBeOverwritten--;
      }

      if (!newList.isEmpty()) {
        // save the new containerDeploymentInfo objects
        wingsPersistence.save(newList);
      }

      // delete the oldest revisions when the max count is reached.
      if (!deleteSet.isEmpty()) {
        deleteContainerDeploymentInfoAndInstances(deleteSet, instanceType, appId);
      }

      if (!updateList.isEmpty()) {
        // update the lastVisited column so that they would be re-evaluated only after the configured interval.
        Query<ContainerDeploymentInfo> query =
            wingsPersistence.createQuery(ContainerDeploymentInfo.class).field("containerSvcName").in(updateList);
        UpdateOperations<ContainerDeploymentInfo> operations =
            wingsPersistence.createUpdateOperations(ContainerDeploymentInfo.class).set("lastVisited", syncTimestamp);
        wingsPersistence.update(query, operations);
      }
    }
  }

  @Override
  public List<ContainerDeploymentInfo> getContainerDeploymentInfoList(String containerSvcNameNoRevision, String appId) {
    PageRequest<ContainerDeploymentInfo> pageRequest =
        PageRequest.Builder.aPageRequest()
            .addFilter("containerSvcNameNoRevision", Operator.EQ, containerSvcNameNoRevision)
            .addFilter("appId", Operator.EQ, appId)
            .addOrder("containerSvcName", OrderType.ASC)
            .build();
    PageResponse<ContainerDeploymentInfo> response = wingsPersistence.query(ContainerDeploymentInfo.class, pageRequest);
    return response.getResponse();
  }

  @Override
  public List<String> getContainerServiceNames(String containerSvcNameNoRevision, String appId) {
    PageRequest<ContainerDeploymentInfo> pageRequest =
        PageRequest.Builder.aPageRequest()
            .addFilter("containerSvcNameNoRevision", Operator.EQ, containerSvcNameNoRevision)
            .addFilter("appId", Operator.EQ, appId)
            .addFieldsIncluded("containerSvcName")
            .addOrder("containerSvcName", OrderType.ASC)
            .build();
    PageResponse<ContainerDeploymentInfo> response = wingsPersistence.query(ContainerDeploymentInfo.class, pageRequest);
    return response.getResponse()
        .stream()
        .map(ContainerDeploymentInfo::getContainerSvcName)
        .collect(Collectors.toList());
  }

  @Override
  public Set<String> getLeastRecentSyncedContainerDeployments(String appId, long lastSyncTimestamp) {
    // query for the least recently visited 20 container service names (without revision)
    FindOptions findOptions = new FindOptions().limit(20);
    Query query = wingsPersistence.createAuthorizedQuery(ContainerDeploymentInfo.class);
    query.field("appId").equal(appId);
    query.field("lastVisited").lessThan(lastSyncTimestamp);
    query.project("containerSvcNameNoRevision", true);
    query.order("lastVisited").order("containerSvcNameNoRevision");

    List<ContainerDeploymentInfo> list = query.asList(findOptions);
    return list.stream().map(ContainerDeploymentInfo::getContainerSvcNameNoRevision).collect(Collectors.toSet());
  }

  @Override
  public void deleteContainerDeploymentInfoAndInstances(
      Set<String> containerSvcNameSetToBeDeleted, InstanceType instanceType, String appId) {
    Query query = wingsPersistence.createAuthorizedQuery(ContainerDeploymentInfo.class);
    query.field("appId").equal(appId);
    query.field("containerSvcName").in(containerSvcNameSetToBeDeleted);
    wingsPersistence.delete(query);

    String fieldName;
    if (KUBERNETES_CONTAINER_INSTANCE.equals(instanceType)) {
      fieldName = "instanceInfo.controllerName";
    } else if (ECS_CONTAINER_INSTANCE.equals(instanceType)) {
      fieldName = "instanceInfo.serviceName";
    } else {
      String msg = "Unsupported container instanceType:" + instanceType;
      logger.error(msg);
      throw new WingsException(msg);
    }

    query = wingsPersistence.createAuthorizedQuery(Instance.class).disableValidation();
    query.field("appId").equal(appId);
    query.field("instanceType").equal(instanceType);
    query.field(fieldName).in(containerSvcNameSetToBeDeleted);
    wingsPersistence.delete(query);
  }

  @Override
  public void deleteInstancesOfAutoScalingGroups(List<String> autoScalingGroupList, String appId) {
    Query query = wingsPersistence.createAuthorizedQuery(Instance.class).disableValidation();
    query.field("appId").equal(appId);
    query.field("instanceInfo.autoScalingGroupName").in(autoScalingGroupList);
    wingsPersistence.delete(query);
  }

  @Override
  public PageResponse<Instance> list(PageRequest<Instance> pageRequest) {
    return wingsPersistence.query(Instance.class, pageRequest);
  }

  /**
   * returns if the instance in db needs to be updated or not.
   * This method returns true only if the task
   *
   * @param existingInstance
   * @param containerInfo
   * @return
   */
  private boolean shouldUpdateInstance(Instance existingInstance, ContainerInfo containerInfo) {
    if (containerInfo instanceof EcsContainerInfo) {
      EcsContainerInfo existingContainerInfo = (EcsContainerInfo) existingInstance.getInstanceInfo();
      if (existingContainerInfo == null) {
        return true;
      }
      EcsContainerInfo newContainerInfo = (EcsContainerInfo) containerInfo;
      // If the taskDefinitionArn is the same (including the revision), no need to update.
      // We only need to update if the task has been re-assigned to a different task definition.
      return !newContainerInfo.getTaskDefinitionArn().equals(existingContainerInfo.getTaskDefinitionArn());

    } else if (containerInfo instanceof KubernetesContainerInfo) {
      KubernetesContainerInfo existingContainerInfo = (KubernetesContainerInfo) existingInstance.getInstanceInfo();
      if (existingContainerInfo == null) {
        return true;
      }
      KubernetesContainerInfo newContainerInfo = (KubernetesContainerInfo) containerInfo;
      // If the replicationControllerName is the same (including the revision), no need to update.
      // We only need to update if the pod has been re-assigned to a different controller.
      return !newContainerInfo.getControllerName().equals(existingContainerInfo.getControllerName());

    } else {
      throw new WingsException("Unsupported container type" + containerInfo.getClass());
    }
  }

  private Map<InstanceKey, Instance> getCurrentInstancesInDB(
      InstanceType instanceType, String containerSvcNameNoRevision, String appId) {
    Query<Instance> query = wingsPersistence.createAuthorizedQuery(Instance.class).disableValidation();
    Map<InstanceKey, Instance> instanceMap;
    query.field("appId").equal(appId);
    if (instanceType == KUBERNETES_CONTAINER_INSTANCE) {
      query.field("instanceType").equal(KUBERNETES_CONTAINER_INSTANCE);
      query.field("instanceInfo.controllerName").startsWith(containerSvcNameNoRevision);
    } else if (instanceType == ECS_CONTAINER_INSTANCE) {
      query.field("instanceType").equal(ECS_CONTAINER_INSTANCE);
      query.field("instanceInfo.serviceName").startsWith(containerSvcNameNoRevision);
    } else {
      String msg = "Unsupported container instanceType:" + instanceType;
      logger.error(msg);
      throw new WingsException(msg);
    }

    List<Instance> instanceList = query.asList();
    instanceMap = instanceList.stream().collect(toMap(Instance::getContainerInstanceKey, instance -> instance));

    return instanceMap;
  }
}
