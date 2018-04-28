package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.api.ContainerDeploymentInfoWithLabels;
import software.wings.api.ContainerDeploymentInfoWithNames;
import software.wings.api.DeploymentInfo;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.instance.sync.ContainerSync;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.utils.Validator;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author rktummala on 02/03/18
 */
@Singleton
public class ContainerInstanceHandler extends InstanceHandler {
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private ContainerInstanceHelper containerInstanceHelper;
  @Inject private ContainerSync containerSync;

  @Override
  public void syncInstances(String appId, String infraMappingId) throws HarnessException {
    // Key - containerSvcName, Value - Instances
    Multimap<String, Instance> containerSvcNameInstanceMap = ArrayListMultimap.create();
    syncInstancesInternal(appId, infraMappingId, containerSvcNameInstanceMap, null);
  }

  protected ContainerInfrastructureMapping getContainerInfraMapping(String appId, String inframappingId)
      throws HarnessException {
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, inframappingId);
    Validator.notNullCheck("Infra mapping is null for id:" + inframappingId, infrastructureMapping);

    if (!(infrastructureMapping instanceof ContainerInfrastructureMapping)) {
      String msg = "Incompatible infra mapping type. Expecting container type. Found:"
          + infrastructureMapping.getInfraMappingType();
      logger.error(msg);
      throw new HarnessException(msg);
    }

    return (ContainerInfrastructureMapping) infrastructureMapping;
  }

  /**
   *
   * @param appId
   * @param infraMappingId
   * @param containerSvcNameInstanceMap  key - containerSvcName     value - Instances
   * @throws HarnessException
   */
  protected void syncInstancesInternal(String appId, String infraMappingId,
      Multimap<String, Instance> containerSvcNameInstanceMap, DeploymentInfo newDeploymentInfo)
      throws HarnessException {
    loadContainerSvcNameInstanceMap(appId, infraMappingId, containerSvcNameInstanceMap);

    ContainerInfrastructureMapping containerInfraMapping = getContainerInfraMapping(appId, infraMappingId);

    // This is to handle the case of the instances stored in the new schema.
    if (containerSvcNameInstanceMap.size() > 0) {
      containerSvcNameInstanceMap.keySet().forEach(containerSvcName -> {
        // Get all the instances for the given containerSvcName (In kubernetes, this is replication Controller and in
        // ECS it is taskDefinition)
        ContainerSyncResponse instanceSyncResponse =
            containerSync.getInstances(containerInfraMapping, asList(containerSvcName));
        Validator.notNullCheck("InstanceSyncResponse", instanceSyncResponse);

        List<ContainerInfo> latestContainerInfoList = instanceSyncResponse.getContainerInfoList();

        // Key - containerId(taskId in ECS / podId in Kubernetes), Value - ContainerInfo
        Map<String, ContainerInfo> latestContainerInfoMap = latestContainerInfoList.stream().collect(
            Collectors.toMap(this ::getContainerId, containerInfo -> containerInfo));

        Collection<Instance> instancesInDB = containerSvcNameInstanceMap.get(containerSvcName);

        // Key - containerId (taskId in ECS / podId in Kubernetes), Value - Instance
        Map<String, Instance> instancesInDBMap = Maps.newHashMap();

        // If there are prior instances in db already
        if (isNotEmpty(instancesInDB)) {
          instancesInDB.forEach(instance -> {
            if (instance != null) {
              instancesInDBMap.put(instance.getContainerInstanceKey().getContainerId(), instance);
            }
          });
        }

        SetView<String> instancesToBeUpdated =
            Sets.intersection(latestContainerInfoMap.keySet(), instancesInDBMap.keySet());

        // Find the instances that were yet to be added to db
        SetView<String> instancesToBeAdded =
            Sets.difference(latestContainerInfoMap.keySet(), instancesInDBMap.keySet());

        SetView<String> instancesToBeDeleted =
            Sets.difference(instancesInDBMap.keySet(), latestContainerInfoMap.keySet());

        instancesToBeUpdated.stream().forEach(containerId -> {
          ContainerInfo containerInfo = latestContainerInfoMap.get(containerId);
          Instance instance = containerInstanceHelper.buildInstanceFromContainerInfo(
              containerInfraMapping, containerInfo, newDeploymentInfo);
          instanceService.saveOrUpdate(instance);
        });

        Set<String> instanceIdsToBeDeleted = new HashSet<>();
        instancesToBeDeleted.stream().forEach(ec2InstanceId -> {
          Instance instance = instancesInDBMap.get(ec2InstanceId);
          if (instance != null) {
            instanceIdsToBeDeleted.add(instance.getUuid());
          }
        });

        logger.info("Total no of Container instances found in DB for InfraMappingId: {} and AppId: {}, "
                + "No of instances in DB: {}, No of Running instances: {}, No of instances updated: {}, "
                + "No of instances to be Added: {}, No of instances to be deleted: {}",
            infraMappingId, appId, instancesInDB.size(), latestContainerInfoMap.keySet().size(),
            instancesToBeUpdated.size(), instancesToBeAdded.size(), instanceIdsToBeDeleted.size());
        if (isNotEmpty(instanceIdsToBeDeleted)) {
          instanceService.delete(instanceIdsToBeDeleted);
        }

        if (isNotEmpty(instancesToBeAdded)) {
          instancesToBeAdded.forEach(containerId -> {
            ContainerInfo containerInfo = latestContainerInfoMap.get(containerId);
            Instance instance = containerInstanceHelper.buildInstanceFromContainerInfo(
                containerInfraMapping, containerInfo, newDeploymentInfo);
            instanceService.saveOrUpdate(instance);
          });
        }
      });
    }
  }

  private void loadContainerSvcNameInstanceMap(String appId, String infraMappingId,
      Multimap<String, Instance> containerSvcNameInstanceMap) throws HarnessException {
    List<Instance> instanceListInDBForInfraMapping = getInstances(appId, infraMappingId);
    for (Instance instance : instanceListInDBForInfraMapping) {
      InstanceInfo instanceInfo = instance.getInstanceInfo();
      if (instanceInfo instanceof ContainerInfo) {
        ContainerInfo containerInfo = (ContainerInfo) instanceInfo;
        String containerSvcName = containerInstanceHelper.getContainerSvcName(containerInfo);
        containerSvcNameInstanceMap.put(containerSvcName, instance);
      } else {
        throw new HarnessException("UnSupported instance deploymentInfo type" + instance.getInstanceType().name());
      }
    }
  }

  @Override
  public void handleNewDeployment(DeploymentInfo deploymentInfo) throws HarnessException {
    Multimap<String, Instance> containerSvcNameInstanceMap = ArrayListMultimap.create();

    if (deploymentInfo instanceof ContainerDeploymentInfoWithLabels) {
      ContainerDeploymentInfoWithLabels containerDeploymentInfo = (ContainerDeploymentInfoWithLabels) deploymentInfo;

      ContainerInfrastructureMapping containerInfraMapping =
          getContainerInfraMapping(deploymentInfo.getAppId(), deploymentInfo.getInfraMappingId());
      Set<String> controllerNames =
          containerSync.getControllerNames(containerInfraMapping, containerDeploymentInfo.getLabels());
      controllerNames.stream().forEach(containerSvcName -> containerSvcNameInstanceMap.put(containerSvcName, null));

    } else if (deploymentInfo instanceof ContainerDeploymentInfoWithNames) {
      ContainerDeploymentInfoWithNames containerDeploymentInfo = (ContainerDeploymentInfoWithNames) deploymentInfo;
      containerDeploymentInfo.getContainerSvcNameSet().stream().forEach(
          containerSvcName -> containerSvcNameInstanceMap.put(containerSvcName, null));
    } else {
      throw new HarnessException("Incompatible deployment info type: " + deploymentInfo.getClass().getName());
    }

    syncInstancesInternal(
        deploymentInfo.getAppId(), deploymentInfo.getInfraMappingId(), containerSvcNameInstanceMap, deploymentInfo);
  }

  private String getContainerId(ContainerInfo containerInfo) {
    if (containerInfo instanceof KubernetesContainerInfo) {
      KubernetesContainerInfo kubernetesContainerInfo = (KubernetesContainerInfo) containerInfo;
      return kubernetesContainerInfo.getPodName();
    } else if (containerInfo instanceof EcsContainerInfo) {
      EcsContainerInfo ecsContainerInfo = (EcsContainerInfo) containerInfo;
      return ecsContainerInfo.getTaskArn();
    } else {
      String msg = "Unsupported container instance type:" + containerInfo;
      logger.error(msg);
      throw new WingsException(msg);
    }
  }
}
