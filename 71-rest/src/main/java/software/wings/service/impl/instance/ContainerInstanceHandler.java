package software.wings.service.impl.instance;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.FeatureName.STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_CONTAINER_DEPLOYMENTS;
import static software.wings.beans.container.Label.Builder.aLabel;
import static software.wings.service.impl.instance.InstanceSyncFlow.NEW_DEPLOYMENT;
import static software.wings.service.impl.instance.InstanceSyncFlow.PERPETUAL_TASK;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.exception.GeneralException;
import io.harness.exception.K8sPodSyncException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.k8s.model.K8sContainer;
import io.harness.k8s.model.K8sPod;

import software.wings.api.CommandStepExecutionSummary;
import software.wings.api.ContainerDeploymentInfoWithLabels;
import software.wings.api.ContainerDeploymentInfoWithNames;
import software.wings.api.ContainerServiceData;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.HelmSetupExecutionSummary;
import software.wings.api.K8sDeploymentInfo;
import software.wings.api.KubernetesSteadyStateCheckExecutionSummary;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.k8s.K8sExecutionSummary;
import software.wings.api.ondemandrollback.OnDemandRollbackInfo;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.FeatureName;
import software.wings.beans.HelmExecutionSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactKeys;
import software.wings.beans.container.Label;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.K8sContainerInfo;
import software.wings.beans.infrastructure.instance.info.K8sPodInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.PodInstanceKey;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.K8sDeploymentKey;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.k8s.response.K8sInstanceSyncResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.ContainerInstanceSyncPerpetualTaskCreator;
import software.wings.service.InstanceSyncPerpetualTaskCreator;
import software.wings.service.impl.ContainerMetadata;
import software.wings.service.impl.ContainerMetadataType;
import software.wings.service.impl.instance.sync.ContainerSync;
import software.wings.service.impl.instance.sync.request.ContainerFilter;
import software.wings.service.impl.instance.sync.request.ContainerSyncRequest;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.sm.ExecutionContext;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.StepExecutionSummary;
import software.wings.sm.states.k8s.K8sStateHelper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author rktummala on 02/03/18
 */
@Singleton
@Slf4j
public class ContainerInstanceHandler extends InstanceHandler implements InstanceSyncByPerpetualTaskHandler {
  @Inject private ContainerSync containerSync;
  @Inject private transient K8sStateHelper k8sStateHelper;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ContainerInstanceSyncPerpetualTaskCreator taskCreator;
  @Inject private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Override
  public void syncInstances(String appId, String infraMappingId, InstanceSyncFlow instanceSyncFlow) {
    // Key - containerSvcName, Value - Instances
    ContainerInfrastructureMapping containerInfraMapping = getContainerInfraMapping(appId, infraMappingId);
    syncInstancesInternal(containerInfraMapping, ArrayListMultimap.create(), null, false, null, instanceSyncFlow);
  }

  private ContainerInfrastructureMapping getContainerInfraMapping(String appId, String inframappingId) {
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, inframappingId);
    notNullCheck("Infra mapping is null for id:" + inframappingId, infrastructureMapping);

    if (!(infrastructureMapping instanceof ContainerInfrastructureMapping)) {
      String msg = "Incompatible infrastructure mapping type found:" + infrastructureMapping.getInfraMappingType();
      log.error(msg);
      throw new GeneralException(msg);
    }

    return (ContainerInfrastructureMapping) infrastructureMapping;
  }

  private void syncInstancesInternal(ContainerInfrastructureMapping containerInfraMapping,
      Multimap<ContainerMetadata, Instance> containerMetadataInstanceMap,
      List<DeploymentSummary> newDeploymentSummaries, boolean rollback, DelegateResponseData responseData,
      InstanceSyncFlow instanceSyncFlow) {
    String appId = containerInfraMapping.getAppId();
    Map<ContainerMetadata, DeploymentSummary> deploymentSummaryMap =
        getDeploymentSummaryMap(newDeploymentSummaries, containerMetadataInstanceMap, containerInfraMapping);

    loadContainerSvcNameInstanceMap(containerInfraMapping, containerMetadataInstanceMap);

    log.info("Found {} containerSvcNames for app {} and infraMapping",
        containerMetadataInstanceMap != null ? containerMetadataInstanceMap.size() : 0, appId);

    if (containerMetadataInstanceMap == null) {
      return;
    }

    // This is to handle the case of the instances stored in the new schema.
    if (containerMetadataInstanceMap.size() > 0) {
      for (ContainerMetadata containerMetadata : containerMetadataInstanceMap.keySet()) {
        Collection<Instance> instancesInDB = Optional.ofNullable(containerMetadataInstanceMap.get(containerMetadata))
                                                 .orElse(emptyList())
                                                 .stream()
                                                 .filter(Objects::nonNull)
                                                 .collect(toList());

        if (containerMetadata.getType() == ContainerMetadataType.K8S) {
          log.info("Found {} instances in DB for app {} and releaseName {}", instancesInDB.size(), appId,
              containerMetadata.getReleaseName());

          handleK8sInstances(responseData, instanceSyncFlow, containerInfraMapping, deploymentSummaryMap,
              containerMetadata, instancesInDB);
        } else {
          if (responseData != null && instanceSyncFlow == PERPETUAL_TASK) {
            ContainerSyncResponse syncResponse = (ContainerSyncResponse) responseData;
            if (isNotEmpty(syncResponse.getControllerName())
                && !syncResponse.getControllerName().equals(containerMetadata.getContainerServiceName())) {
              continue;
            }
          }
          log.info("Found {} instances in DB for app {} and containerServiceName {}", instancesInDB.size(), appId,
              containerMetadata.getContainerServiceName());

          handleContainerServiceInstances(rollback, responseData, instanceSyncFlow, containerInfraMapping,
              deploymentSummaryMap, containerMetadata, instancesInDB);
        }
      }
    }
  }

  private void handleContainerServiceInstances(boolean rollback, DelegateResponseData responseData,
      InstanceSyncFlow instanceSyncFlow, ContainerInfrastructureMapping containerInfraMapping,
      Map<ContainerMetadata, DeploymentSummary> deploymentSummaryMap, ContainerMetadata containerMetadata,
      Collection<Instance> instancesInDB) {
    // Get all the instances for the given containerSvcName (In kubernetes, this is replication Controller and in
    // ECS it is taskDefinition)
    List<ContainerInfo> latestContainerInfoList =
        getContainerInfos(responseData, instanceSyncFlow, containerInfraMapping, containerMetadata);
    log.info("Found {} instances from remote server for app {} and containerSvcName {}", latestContainerInfoList.size(),
        containerInfraMapping.getAppId(), containerMetadata.getContainerServiceName());
    processContainerServiceInstances(rollback, containerInfraMapping, deploymentSummaryMap, containerMetadata,
        instancesInDB, latestContainerInfoList);
  }

  private void processContainerServiceInstances(boolean rollback, ContainerInfrastructureMapping containerInfraMapping,
      Map<ContainerMetadata, DeploymentSummary> deploymentSummaryMap, ContainerMetadata containerMetadata,
      Collection<Instance> instancesInDB, List<ContainerInfo> latestContainerInfoList) {
    // Key - containerId(taskId in ECS / podId+namespace in Kubernetes), Value - ContainerInfo
    Map<String, ContainerInfo> latestContainerInfoMap = new HashMap<>();
    HelmChartInfo helmChartInfo = getContainerHelmChartInfo(deploymentSummaryMap.get(containerMetadata), instancesInDB);
    for (ContainerInfo info : latestContainerInfoList) {
      if (info instanceof KubernetesContainerInfo) {
        KubernetesContainerInfo k8sInfo = (KubernetesContainerInfo) info;
        String namespace = isNotBlank(k8sInfo.getNamespace()) ? k8sInfo.getNamespace() : "";
        String releaseName = getReleaseNameKey(k8sInfo);
        latestContainerInfoMap.put(k8sInfo.getPodName() + namespace + releaseName, info);
        setHelmChartInfoToContainerInfo(helmChartInfo, k8sInfo);
      } else {
        latestContainerInfoMap.put(((EcsContainerInfo) info).getTaskArn(), info);
      }
    }

    // Key - containerId (taskId in ECS / podId in Kubernetes), Value - Instance
    Map<String, Instance> instancesInDBMap = new HashMap<>();

    // If there are prior instances in db already
    for (Instance instance : instancesInDB) {
      ContainerInstanceKey key = instance.getContainerInstanceKey();
      String namespace = isNotBlank(key.getNamespace()) ? key.getNamespace() : "";
      String releaseName = getReleaseNameKey(instance.getInstanceInfo());
      String instanceMapKey = key.getContainerId() + namespace + releaseName;

      if (!instancesInDBMap.containsKey(instanceMapKey)) {
        instancesInDBMap.put(instanceMapKey, instance);
      } else {
        instancesInDBMap.put(instanceMapKey + instance.getUuid(), instance);
      }
    }

    // Find the instances that were yet to be added to db
    SetView<String> instancesToBeAdded = Sets.difference(latestContainerInfoMap.keySet(), instancesInDBMap.keySet());

    SetView<String> instancesToBeDeleted = Sets.difference(instancesInDBMap.keySet(), latestContainerInfoMap.keySet());

    Set<String> instanceIdsToBeDeleted = new HashSet<>();
    for (String containerId : instancesToBeDeleted) {
      Instance instance = instancesInDBMap.get(containerId);
      if (instance != null) {
        instanceIdsToBeDeleted.add(instance.getUuid());
      }
    }

    log.info("Instances to be added {}", instancesToBeAdded.size());
    log.info("Instances to be deleted {}", instanceIdsToBeDeleted.size());

    log.info("Total number of Container instances found in DB for ContainerSvcName: {}, Namespace {} and AppId: {}, "
            + "No of instances in DB: {}, No of Running instances: {}, "
            + "No of instances to be Added: {}, No of instances to be deleted: {}",
        containerMetadata.getContainerServiceName(), containerMetadata.getNamespace(), containerInfraMapping.getAppId(),
        instancesInDB.size(), latestContainerInfoMap.keySet().size(), instancesToBeAdded.size(),
        instanceIdsToBeDeleted.size());
    if (isNotEmpty(instanceIdsToBeDeleted)) {
      instanceService.delete(instanceIdsToBeDeleted);
    }

    DeploymentSummary deploymentSummary;
    if (isNotEmpty(instancesToBeAdded)) {
      // newDeploymentInfo would be null in case of sync job.
      if (!deploymentSummaryMap.containsKey(containerMetadata) && isNotEmpty(instancesInDB)) {
        Optional<Instance> instanceWithExecutionInfoOptional = getInstanceWithExecutionInfo(instancesInDB);
        if (!instanceWithExecutionInfoOptional.isPresent()) {
          log.warn("Couldn't find an instance from a previous deployment");
          return;
        }

        DeploymentSummary deploymentSummaryFromPrevious =
            DeploymentSummary.builder().deploymentInfo(ContainerDeploymentInfoWithNames.builder().build()).build();
        // We pick one of the existing instances from db for the same controller / task definition
        generateDeploymentSummaryFromInstance(instanceWithExecutionInfoOptional.get(), deploymentSummaryFromPrevious);
        deploymentSummary = deploymentSummaryFromPrevious;
      } else {
        deploymentSummary =
            getDeploymentSummaryForInstanceCreation(deploymentSummaryMap.get(containerMetadata), rollback);
      }

      for (String containerId : instancesToBeAdded) {
        ContainerInfo containerInfo = latestContainerInfoMap.get(containerId);
        Instance instance = buildInstanceFromContainerInfo(containerInfraMapping, containerInfo, deploymentSummary);
        instanceService.save(instance);
      }
    }

    // Update the existing instances helm chart info
    if (deploymentSummaryMap.containsKey(containerMetadata)) {
      deploymentSummary = deploymentSummaryMap.get(containerMetadata);
      if (deploymentSummary.getDeploymentInfo() instanceof ContainerDeploymentInfoWithLabels) {
        ContainerDeploymentInfoWithLabels deploymentInfo =
            (ContainerDeploymentInfoWithLabels) deploymentSummary.getDeploymentInfo();
        SetView<String> instancesToBeUpdated =
            Sets.intersection(latestContainerInfoMap.keySet(), instancesInDBMap.keySet());
        for (String instanceId : instancesToBeUpdated) {
          Instance instance = instancesInDBMap.get(instanceId);
          if (updateHelmChartInfoForContainerInstances(deploymentInfo, instance)) {
            instanceService.saveOrUpdate(instance);
          }
        }
      }
    }
  }

  private String getReleaseNameKey(InstanceInfo instanceInfo) {
    String releaseName = null;
    if (instanceInfo instanceof KubernetesContainerInfo) {
      releaseName = ((KubernetesContainerInfo) instanceInfo).getReleaseName();
    } else if (instanceInfo instanceof K8sPodInfo) {
      releaseName = ((K8sPodInfo) instanceInfo).getReleaseName();
    }

    return isNotBlank(releaseName) ? releaseName : "";
  }

  private List<ContainerInfo> getContainerInfos(DelegateResponseData responseData, InstanceSyncFlow instanceSyncFlow,
      ContainerInfrastructureMapping containerInfraMapping, ContainerMetadata containerMetadata) {
    ContainerSyncResponse instanceSyncResponse = null;
    if (PERPETUAL_TASK != instanceSyncFlow) {
      instanceSyncResponse = containerSync.getInstances(containerInfraMapping, singletonList(containerMetadata));
    } else if (responseData instanceof ContainerSyncResponse) {
      instanceSyncResponse = (ContainerSyncResponse) responseData;
    }

    if (instanceSyncResponse == null) {
      throw new GeneralException(
          "InstanceSyncResponse is null for containerSvcName: " + containerMetadata.getContainerServiceName());
    }

    return Optional.ofNullable(instanceSyncResponse.getContainerInfoList()).orElse(emptyList());
  }

  private void handleK8sInstances(DelegateResponseData responseData, InstanceSyncFlow instanceSyncFlow,
      ContainerInfrastructureMapping containerInfraMapping,
      Map<ContainerMetadata, DeploymentSummary> deploymentSummaryMap, ContainerMetadata containerMetadata,
      Collection<Instance> instancesInDB) {
    List<K8sPod> k8sPods = getK8sPods(responseData, instanceSyncFlow, containerInfraMapping, containerMetadata);
    processK8sPodsInstances(containerInfraMapping, containerMetadata, instancesInDB, deploymentSummaryMap, k8sPods);
  }

  private List<K8sPod> getK8sPods(DelegateResponseData responseData, InstanceSyncFlow instanceSyncFlow,
      ContainerInfrastructureMapping containerInfraMapping, ContainerMetadata containerMetadata) {
    if (PERPETUAL_TASK != instanceSyncFlow) {
      return getK8sPodsFromDelegate(containerInfraMapping, containerMetadata);
    } else if (responseData instanceof K8sTaskExecutionResponse) {
      K8sTaskExecutionResponse k8sTaskExecutionResponse = (K8sTaskExecutionResponse) responseData;
      K8sInstanceSyncResponse k8sInstanceSyncResponse =
          (K8sInstanceSyncResponse) k8sTaskExecutionResponse.getK8sTaskResponse();
      return k8sInstanceSyncResponse.getK8sPodInfoList();
    }
    return Collections.emptyList();
  }

  @VisibleForTesting
  HelmChartInfo getContainerHelmChartInfo(DeploymentSummary deploymentSummary, Collection<Instance> existingInstances) {
    return Optional.ofNullable(deploymentSummary)
        .map(DeploymentSummary::getDeploymentInfo)
        .filter(ContainerDeploymentInfoWithLabels.class ::isInstance)
        .map(ContainerDeploymentInfoWithLabels.class ::cast)
        .map(ContainerDeploymentInfoWithLabels::getHelmChartInfo)
        .orElseGet(()
                       -> existingInstances.stream()
                              .sorted(Comparator.comparingLong(Instance::getLastDeployedAt).reversed())
                              .map(Instance::getInstanceInfo)
                              .filter(KubernetesContainerInfo.class ::isInstance)
                              .map(KubernetesContainerInfo.class ::cast)
                              .map(KubernetesContainerInfo::getHelmChartInfo)
                              .filter(Objects::nonNull)
                              .findFirst()
                              .orElse(null));
  }

  @VisibleForTesting
  void setHelmChartInfoToContainerInfo(HelmChartInfo helmChartInfo, ContainerInfo k8sInfo) {
    Optional.ofNullable(helmChartInfo).ifPresent(chartInfo -> {
      if (KubernetesContainerInfo.class == k8sInfo.getClass()) {
        ((KubernetesContainerInfo) k8sInfo).setHelmChartInfo(helmChartInfo);
      } else if (K8sPodInfo.class == k8sInfo.getClass()) {
        ((K8sPodInfo) k8sInfo).setHelmChartInfo(helmChartInfo);
      }
    });
  }

  private boolean updateHelmChartInfoForContainerInstances(
      ContainerDeploymentInfoWithLabels deploymentInfo, Instance instance) {
    if (!(instance.getInstanceInfo() instanceof KubernetesContainerInfo)) {
      return false;
    }

    KubernetesContainerInfo containerInfo = (KubernetesContainerInfo) instance.getInstanceInfo();
    if (deploymentInfo.getHelmChartInfo() != null
        && !deploymentInfo.getHelmChartInfo().equals(containerInfo.getHelmChartInfo())) {
      containerInfo.setHelmChartInfo(deploymentInfo.getHelmChartInfo());
      return true;
    }

    return false;
  }

  private List<K8sPod> getK8sPodsFromDelegate(
      ContainerInfrastructureMapping containerInfraMapping, ContainerMetadata containerMetadata) {
    try {
      return k8sStateHelper.getPodList(
          containerInfraMapping, containerMetadata.getNamespace(), containerMetadata.getReleaseName());
    } catch (Exception e) {
      throw new K8sPodSyncException(format("Exception in fetching podList for release %s, namespace %s",
                                        containerMetadata.getReleaseName(), containerMetadata.getNamespace()),
          e);
    }
  }
  private String getImageInStringFormat(Instance instance) {
    if (instance.getInstanceInfo() instanceof K8sPodInfo) {
      return emptyIfNull(((K8sPodInfo) instance.getInstanceInfo()).getContainers())
          .stream()
          .map(K8sContainerInfo::getImage)
          .collect(Collectors.joining());
    }
    return EMPTY;
  }

  private String getImageInStringFormat(K8sPod pod) {
    return emptyIfNull(pod.getContainerList()).stream().map(K8sContainer::getImage).collect(Collectors.joining());
  }

  private void processK8sPodsInstances(ContainerInfrastructureMapping containerInfraMapping,
      ContainerMetadata containerMetadata, Collection<Instance> instancesInDB,
      Map<ContainerMetadata, DeploymentSummary> deploymentSummaryMap, List<K8sPod> currentPods) {
    Map<String, K8sPod> currentPodsMap = new HashMap<>();
    Map<String, Instance> dbPodMap = new HashMap<>();

    currentPods.forEach(podInfo
        -> currentPodsMap.put(podInfo.getName() + podInfo.getNamespace() + getImageInStringFormat(podInfo), podInfo));
    instancesInDB.forEach(podInstance
        -> dbPodMap.put(podInstance.getPodInstanceKey().getPodName() + podInstance.getPodInstanceKey().getNamespace()
                + getImageInStringFormat(podInstance),
            podInstance));

    SetView<String> instancesToBeAdded = Sets.difference(currentPodsMap.keySet(), dbPodMap.keySet());
    SetView<String> instancesToBeDeleted = Sets.difference(dbPodMap.keySet(), currentPodsMap.keySet());

    Set<String> instanceIdsToBeDeleted =
        instancesToBeDeleted.stream().map(instancePodName -> dbPodMap.get(instancePodName).getUuid()).collect(toSet());

    log.info(
        "[InstanceSync for namespace {} release {}] Got {} running Pods. InstancesToBeAdded:{} InstancesToBeDeleted:{}",
        containerMetadata.getNamespace(), containerMetadata.getReleaseName(), currentPods.size(),
        instancesToBeAdded.size(), instanceIdsToBeDeleted.size());

    if (isNotEmpty(instanceIdsToBeDeleted)) {
      instanceService.delete(instanceIdsToBeDeleted);
      log.info("Instances to be deleted {}", instanceIdsToBeDeleted.size());
    }

    DeploymentSummary deploymentSummary = deploymentSummaryMap.get(containerMetadata);
    for (String podName : instancesToBeAdded) {
      if (deploymentSummary == null && !instancesInDB.isEmpty()) {
        deploymentSummary =
            DeploymentSummary.builder().deploymentInfo(ContainerDeploymentInfoWithNames.builder().build()).build();
        generateDeploymentSummaryFromInstance(instancesInDB.stream().findFirst().get(), deploymentSummary);
      }
      HelmChartInfo helmChartInfo =
          getK8sPodHelmChartInfo(deploymentSummary, currentPodsMap.get(podName), instancesInDB);
      Instance instance =
          buildInstanceFromPodInfo(containerInfraMapping, currentPodsMap.get(podName), deploymentSummary);
      ContainerInfo containerInfo = (ContainerInfo) instance.getInstanceInfo();
      setHelmChartInfoToContainerInfo(helmChartInfo, containerInfo);
      instanceService.saveOrUpdate(instance);
    }

    log.info("Instances to be added {}", instancesToBeAdded.size());

    if (deploymentSummaryMap.get(containerMetadata) != null) {
      deploymentSummary = deploymentSummaryMap.get(containerMetadata);
      SetView<String> instancesToBeUpdated = Sets.intersection(currentPodsMap.keySet(), dbPodMap.keySet());

      for (String podName : instancesToBeUpdated) {
        Instance instanceToBeUpdated = dbPodMap.get(podName);
        K8sPod k8sPod = currentPodsMap.get(podName);
        String deploymentWorkflowName = Optional.ofNullable(deploymentSummary.getWorkflowExecutionName()).orElse("");
        if (deploymentWorkflowName.equals(instanceToBeUpdated.getLastWorkflowExecutionName())
            && updateHelmChartInfoForExistingK8sPod(instanceToBeUpdated, k8sPod, deploymentSummary)) {
          instanceService.saveOrUpdate(instanceToBeUpdated);
        }
      }
    }
  }

  private HelmChartInfo getK8sPodHelmChartInfo(
      DeploymentSummary deploymentSummary, K8sPod pod, Collection<Instance> instances) {
    if (deploymentSummary != null && deploymentSummary.getDeploymentInfo() instanceof K8sDeploymentInfo) {
      K8sDeploymentInfo deploymentInfo = (K8sDeploymentInfo) deploymentSummary.getDeploymentInfo();
      return StringUtils.equals(pod.getColor(), deploymentInfo.getBlueGreenStageColor())
          ? deploymentInfo.getHelmChartInfo()
          : null;
    }

    return instances.stream()
        .sorted(Comparator.comparingLong(Instance::getLastDeployedAt).reversed())
        .map(Instance::getInstanceInfo)
        .filter(K8sPodInfo.class ::isInstance)
        .map(K8sPodInfo.class ::cast)
        .filter(podInfo -> StringUtils.equals(podInfo.getBlueGreenColor(), pod.getColor()))
        .findFirst()
        .map(K8sPodInfo::getHelmChartInfo)
        .orElse(null);
  }

  private boolean updateHelmChartInfoForExistingK8sPod(
      Instance instance, K8sPod pod, DeploymentSummary deploymentSummary) {
    if (!(instance.getInstanceInfo() instanceof K8sPodInfo)
        || !(deploymentSummary.getDeploymentInfo() instanceof K8sDeploymentInfo)) {
      return false;
    }

    K8sPodInfo k8sPodInfo = (K8sPodInfo) instance.getInstanceInfo();
    K8sDeploymentInfo deploymentInfo = (K8sDeploymentInfo) deploymentSummary.getDeploymentInfo();
    if (StringUtils.equals(deploymentInfo.getBlueGreenStageColor(), pod.getColor())) {
      if (deploymentInfo.getHelmChartInfo() != null
          && !deploymentInfo.getHelmChartInfo().equals(k8sPodInfo.getHelmChartInfo())) {
        k8sPodInfo.setHelmChartInfo(deploymentInfo.getHelmChartInfo());
        return true;
      }
    }

    return false;
  }

  @VisibleForTesting
  Map<ContainerMetadata, DeploymentSummary> getDeploymentSummaryMap(List<DeploymentSummary> newDeploymentSummaries,
      Multimap<ContainerMetadata, Instance> containerInstances, ContainerInfrastructureMapping containerInfraMapping) {
    if (EmptyPredicate.isEmpty(newDeploymentSummaries)) {
      return emptyMap();
    }

    Map<ContainerMetadata, DeploymentSummary> deploymentSummaryMap = new HashMap<>();

    if (newDeploymentSummaries.stream().iterator().next().getDeploymentInfo()
            instanceof ContainerDeploymentInfoWithLabels) {
      Map<String, String> labelMap = new HashMap<>();
      for (DeploymentSummary deploymentSummary : newDeploymentSummaries) {
        ContainerDeploymentInfoWithLabels containerDeploymentInfo =
            (ContainerDeploymentInfoWithLabels) deploymentSummary.getDeploymentInfo();
        containerDeploymentInfo.getLabels().forEach(
            labelEntry -> labelMap.put(labelEntry.getName(), labelEntry.getValue()));

        String namespace = containerInfraMapping.getNamespace();
        if (ExpressionEvaluator.containsVariablePattern(namespace)) {
          namespace = containerDeploymentInfo.getNamespace();
        }

        boolean isControllerNamesRetrievable = emptyIfNull(containerDeploymentInfo.getContainerInfoList())
                                                   .stream()
                                                   .map(io.harness.container.ContainerInfo::getWorkloadName)
                                                   .anyMatch(EmptyPredicate::isNotEmpty);
        /*
         We need controller names only if release name is not set
         */
        if (isControllerNamesRetrievable || isEmpty(containerDeploymentInfo.getContainerInfoList())) {
          Set<String> controllerNames = containerSync.getControllerNames(containerInfraMapping, labelMap, namespace);

          log.info(
              "Number of controllers returned for executionId [{}], inframappingId [{}], appId [{}] from labels: {}",
              newDeploymentSummaries.iterator().next().getWorkflowExecutionId(), containerInfraMapping.getUuid(),
              newDeploymentSummaries.iterator().next().getAppId(), controllerNames.size());

          for (String controllerName : controllerNames) {
            ContainerMetadata containerMetadata = ContainerMetadata.builder()
                                                      .containerServiceName(controllerName)
                                                      .namespace(namespace)
                                                      .releaseName(containerDeploymentInfo.getReleaseName())
                                                      .build();
            deploymentSummaryMap.put(containerMetadata, deploymentSummary);
            containerInstances.put(containerMetadata, null);
          }
        } else {
          ContainerMetadata containerMetadata = ContainerMetadata.builder()
                                                    .namespace(namespace)
                                                    .releaseName(containerDeploymentInfo.getReleaseName())
                                                    .build();
          deploymentSummaryMap.put(containerMetadata, deploymentSummary);
          containerInstances.put(containerMetadata, null);
        }
      }
    } else if (newDeploymentSummaries.stream().iterator().next().getDeploymentInfo() instanceof K8sDeploymentInfo) {
      newDeploymentSummaries.forEach(deploymentSummary -> {
        K8sDeploymentInfo deploymentInfo = (K8sDeploymentInfo) deploymentSummary.getDeploymentInfo();

        String releaseName = deploymentInfo.getReleaseName();
        Set<String> namespaces = new HashSet<>();
        if (isNotBlank(deploymentInfo.getNamespace())) {
          namespaces.add(deploymentInfo.getNamespace());
        }

        if (isNotEmpty(deploymentInfo.getNamespaces())) {
          namespaces.addAll(deploymentInfo.getNamespaces());
        }

        for (String namespace : namespaces) {
          deploymentSummaryMap.put(ContainerMetadata.builder()
                                       .type(ContainerMetadataType.K8S)
                                       .releaseName(releaseName)
                                       .namespace(namespace)
                                       .build(),
              deploymentSummary);
        }
      });
    } else {
      newDeploymentSummaries.forEach(deploymentSummary -> {
        ContainerDeploymentInfoWithNames deploymentInfo =
            (ContainerDeploymentInfoWithNames) deploymentSummary.getDeploymentInfo();
        deploymentSummaryMap.put(ContainerMetadata.builder()
                                     .containerServiceName(deploymentInfo.getContainerSvcName())
                                     .namespace(deploymentInfo.getNamespace())
                                     .build(),
            deploymentSummary);
      });
    }

    return deploymentSummaryMap;
  }
  private void loadContainerSvcNameInstanceMap(
      ContainerInfrastructureMapping containerInfraMapping, Multimap<ContainerMetadata, Instance> instanceMap) {
    String appId = containerInfraMapping.getAppId();
    List<Instance> instanceListInDBForInfraMapping = getInstances(appId, containerInfraMapping.getUuid());
    log.info("Found {} instances for app {}", instanceListInDBForInfraMapping.size(), appId);
    for (Instance instance : instanceListInDBForInfraMapping) {
      InstanceInfo instanceInfo = instance.getInstanceInfo();
      if (instanceInfo instanceof ContainerInfo) {
        ContainerInfo containerInfo = (ContainerInfo) instanceInfo;
        String containerSvcName = getContainerSvcName(containerInfo);
        String namespace = null;
        String releaseName = null;
        if (containerInfo instanceof KubernetesContainerInfo) {
          namespace = ((KubernetesContainerInfo) containerInfo).getNamespace();
          releaseName = ((KubernetesContainerInfo) containerInfo).getReleaseName();
        } else if (containerInfo instanceof K8sPodInfo) {
          namespace = ((K8sPodInfo) containerInfo).getNamespace();
          releaseName = ((K8sPodInfo) containerInfo).getReleaseName();
        }
        ContainerMetadataType type = containerInfo instanceof K8sPodInfo ? ContainerMetadataType.K8S : null;
        instanceMap.put(ContainerMetadata.builder()
                            .type(type)
                            .containerServiceName(containerSvcName)
                            .namespace(namespace)
                            .releaseName(isNotEmpty(releaseName) ? releaseName : null)
                            .build(),
            instance);
      } else {
        throw new GeneralException("UnSupported instance deploymentInfo type" + instance.getInstanceType().name());
      }
    }
  }

  @Override
  public void handleNewDeployment(
      List<DeploymentSummary> deploymentSummaries, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo) {
    Multimap<ContainerMetadata, Instance> containerSvcNameInstanceMap = ArrayListMultimap.create();

    if (isEmpty(deploymentSummaries)) {
      return;
    }

    String infraMappingId = deploymentSummaries.iterator().next().getInfraMappingId();
    String appId = deploymentSummaries.iterator().next().getAppId();
    log.info("Handling new container deployment for inframappingId [{}]", infraMappingId);
    validateDeploymentInfos(deploymentSummaries);

    if (deploymentSummaries.iterator().next().getDeploymentInfo() instanceof ContainerDeploymentInfoWithNames) {
      deploymentSummaries.forEach(deploymentSummary -> {
        ContainerDeploymentInfoWithNames deploymentInfo =
            (ContainerDeploymentInfoWithNames) deploymentSummary.getDeploymentInfo();
        containerSvcNameInstanceMap.put(ContainerMetadata.builder()
                                            .containerServiceName(deploymentInfo.getContainerSvcName())
                                            .namespace(deploymentInfo.getNamespace())
                                            .build(),
            null);
      });
    } else if (deploymentSummaries.iterator().next().getDeploymentInfo() instanceof K8sDeploymentInfo) {
      deploymentSummaries.forEach(deploymentSummary -> {
        K8sDeploymentInfo deploymentInfo = (K8sDeploymentInfo) deploymentSummary.getDeploymentInfo();

        String releaseName = deploymentInfo.getReleaseName();
        Set<String> namespaces = new HashSet<>();
        if (isNotBlank(deploymentInfo.getNamespace())) {
          namespaces.add(deploymentInfo.getNamespace());
        }

        if (isNotEmpty(deploymentInfo.getNamespaces())) {
          namespaces.addAll(deploymentInfo.getNamespaces());
        }

        for (String namespace : namespaces) {
          containerSvcNameInstanceMap.put(ContainerMetadata.builder()
                                              .type(ContainerMetadataType.K8S)
                                              .releaseName(releaseName)
                                              .namespace(namespace)
                                              .build(),
              null);
        }
      });
    }

    ContainerInfrastructureMapping containerInfraMapping = getContainerInfraMapping(appId, infraMappingId);
    syncInstancesInternal(
        containerInfraMapping, containerSvcNameInstanceMap, deploymentSummaries, rollback, null, NEW_DEPLOYMENT);
  }

  @Override
  public FeatureName getFeatureFlagToStopIteratorBasedInstanceSync() {
    return STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_CONTAINER_DEPLOYMENTS;
  }

  private void validateDeploymentInfos(List<DeploymentSummary> deploymentSummaries) {
    for (DeploymentSummary deploymentSummary : deploymentSummaries) {
      DeploymentInfo deploymentInfo = deploymentSummary.getDeploymentInfo();
      if (!(deploymentInfo instanceof ContainerDeploymentInfoWithNames)
          && !(deploymentInfo instanceof ContainerDeploymentInfoWithLabels)
          && !(deploymentInfo instanceof K8sDeploymentInfo)) {
        throw new GeneralException("Incompatible deployment info type: " + deploymentInfo);
      }
    }
  }

  public boolean isContainerDeployment(InfrastructureMapping infrastructureMapping) {
    return infrastructureMapping instanceof ContainerInfrastructureMapping;
  }

  @Override
  public Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
      InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact) {
    PhaseStepExecutionSummary phaseStepExecutionSummary = phaseStepExecutionData.getPhaseStepExecutionSummary();

    if (phaseStepExecutionSummary == null) {
      if (log.isDebugEnabled()) {
        log.debug("PhaseStepExecutionSummary is null for stateExecutionInstanceId: " + stateExecutionInstanceId);
      }
      return Optional.empty();
    }
    List<StepExecutionSummary> stepExecutionSummaryList = phaseStepExecutionSummary.getStepExecutionSummaryList();
    // This was observed when the "deploy containers" step was executed in rollback and no commands were
    // executed since setup failed.
    if (stepExecutionSummaryList == null) {
      if (log.isDebugEnabled()) {
        log.debug("StepExecutionSummaryList is null for stateExecutionInstanceId: " + stateExecutionInstanceId);
      }
      return Optional.empty();
    }

    for (StepExecutionSummary stepExecutionSummary : stepExecutionSummaryList) {
      if (stepExecutionSummary != null) {
        if (stepExecutionSummary instanceof CommandStepExecutionSummary) {
          CommandStepExecutionSummary commandStepExecutionSummary = (CommandStepExecutionSummary) stepExecutionSummary;
          String clusterName = commandStepExecutionSummary.getClusterName();
          Set<String> containerSvcNameSet = Sets.newHashSet();

          if (checkIfContainerServiceDataAvailable(
                  stateExecutionInstanceId, commandStepExecutionSummary, containerSvcNameSet)) {
            return Optional.empty();
          }

          List<DeploymentInfo> containerDeploymentInfoWithNames = getContainerDeploymentInfos(
              clusterName, commandStepExecutionSummary.getNamespace(), commandStepExecutionSummary);

          return Optional.of(containerDeploymentInfoWithNames);

        } else if (stepExecutionSummary instanceof K8sExecutionSummary) {
          return Optional.of(singletonList(getK8sDeploymentInfo((K8sExecutionSummary) stepExecutionSummary)));
        } else if (stepExecutionSummary instanceof HelmSetupExecutionSummary
            || stepExecutionSummary instanceof KubernetesSteadyStateCheckExecutionSummary) {
          if (!(infrastructureMapping instanceof ContainerInfrastructureMapping)) {
            log.warn("Inframapping is not container type. cannot proceed for state execution instance: {}",
                stateExecutionInstanceId);
            return Optional.empty();
          }

          String clusterName = ((ContainerInfrastructureMapping) infrastructureMapping).getClusterName();

          List<Label> labels = new ArrayList<>();

          DeploymentInfo deploymentInfo;
          if (stepExecutionSummary instanceof HelmSetupExecutionSummary) {
            HelmSetupExecutionSummary helmSetupExecutionSummary = (HelmSetupExecutionSummary) stepExecutionSummary;
            labels.add(aLabel().withName("release").withValue(helmSetupExecutionSummary.getReleaseName()).build());
            deploymentInfo =
                getContainerDeploymentInfosWithLabelsForHelm(clusterName, helmSetupExecutionSummary.getNamespace(),
                    labels, helmSetupExecutionSummary, workflowExecution.getHelmExecutionSummary());
          } else {
            KubernetesSteadyStateCheckExecutionSummary kubernetesSteadyStateCheckExecutionSummary =
                (KubernetesSteadyStateCheckExecutionSummary) stepExecutionSummary;
            labels.addAll(kubernetesSteadyStateCheckExecutionSummary.getLabels());
            deploymentInfo = getContainerDeploymentInfosWithLabels(
                clusterName, kubernetesSteadyStateCheckExecutionSummary.getNamespace(), labels);
          }

          if (deploymentInfo == null) {
            return Optional.empty();
          }

          return Optional.of(singletonList(deploymentInfo));
        }
      }
    }
    return Optional.empty();
  }

  private boolean checkIfContainerServiceDataAvailable(String stateExecutionInstanceId,
      CommandStepExecutionSummary commandStepExecutionSummary, Set<String> containerSvcNameSet) {
    if (commandStepExecutionSummary.getOldInstanceData() != null) {
      containerSvcNameSet.addAll(commandStepExecutionSummary.getOldInstanceData()
                                     .stream()
                                     .map(ContainerServiceData::getName)
                                     .collect(toList()));
    }

    if (commandStepExecutionSummary.getNewInstanceData() != null) {
      List<String> newcontainerSvcNames = commandStepExecutionSummary.getNewInstanceData()
                                              .stream()
                                              .map(ContainerServiceData::getName)
                                              .collect(toList());
      containerSvcNameSet.addAll(newcontainerSvcNames);
    }

    // Filter out null values
    List<String> serviceNames = containerSvcNameSet.stream().filter(EmptyPredicate::isNotEmpty).collect(toList());

    if (isEmpty(serviceNames)) {
      log.warn(
          "Both old and new container services are empty. Cannot proceed for phase step for state execution instance: {}",
          stateExecutionInstanceId);
      return true;
    }
    return false;
  }

  private DeploymentInfo getContainerDeploymentInfosWithLabels(
      String clusterName, String namespace, List<Label> labels) {
    return ContainerDeploymentInfoWithLabels.builder()
        .clusterName(clusterName)
        .namespace(namespace)
        .labels(labels)
        .build();
  }

  @VisibleForTesting
  DeploymentInfo getContainerDeploymentInfosWithLabelsForHelm(String clusterName, String namespace, List<Label> labels,
      HelmSetupExecutionSummary executionSummary, HelmExecutionSummary helmExecutionSummary) {
    Integer version = executionSummary.getRollbackVersion() == null ? executionSummary.getNewVersion()
                                                                    : executionSummary.getRollbackVersion();

    if (version == null) {
      return null;
    }

    return ContainerDeploymentInfoWithLabels.builder()
        .clusterName(clusterName)
        .namespace(namespace)
        .labels(labels)
        .newVersion(version.toString())
        .helmChartInfo(helmExecutionSummary.getHelmChartInfo())
        .containerInfoList(helmExecutionSummary.getContainerInfoList())
        .releaseName(helmExecutionSummary.getReleaseName())
        .build();
  }

  private DeploymentInfo getK8sDeploymentInfo(K8sExecutionSummary k8sExecutionSummary) {
    return K8sDeploymentInfo.builder()
        .namespace(k8sExecutionSummary.getNamespace())
        .releaseName(k8sExecutionSummary.getReleaseName())
        .releaseNumber(k8sExecutionSummary.getReleaseNumber())
        .namespaces(k8sExecutionSummary.getNamespaces())
        .helmChartInfo(k8sExecutionSummary.getHelmChartInfo())
        .blueGreenStageColor(k8sExecutionSummary.getBlueGreenStageColor())
        .build();
  }

  private List<DeploymentInfo> getContainerDeploymentInfos(
      String clusterName, String namespace, CommandStepExecutionSummary commandStepExecutionSummary) {
    List<DeploymentInfo> containerDeploymentInfoWithNames = new ArrayList<>();
    addToDeploymentInfoWithNames(
        clusterName, namespace, commandStepExecutionSummary.getNewInstanceData(), containerDeploymentInfoWithNames);
    addToDeploymentInfoWithNames(
        clusterName, namespace, commandStepExecutionSummary.getOldInstanceData(), containerDeploymentInfoWithNames);

    return containerDeploymentInfoWithNames;
  }

  private void addToDeploymentInfoWithNames(String clusterName, String namespace,
      List<ContainerServiceData> containerServiceDataList, List<DeploymentInfo> containerDeploymentInfoWithNames) {
    if (isNotEmpty(containerServiceDataList)) {
      containerServiceDataList.forEach(containerServiceData
          -> containerDeploymentInfoWithNames.add(ContainerDeploymentInfoWithNames.builder()
                                                      .containerSvcName(containerServiceData.getName())
                                                      .uniqueNameIdentifier(containerServiceData.getUniqueIdentifier())
                                                      .clusterName(clusterName)
                                                      .namespace(namespace)
                                                      .build()));
    }
  }

  private Instance buildInstanceFromContainerInfo(
      InfrastructureMapping infraMapping, ContainerInfo containerInfo, DeploymentSummary deploymentSummary) {
    InstanceBuilder builder = buildInstanceBase(null, infraMapping, deploymentSummary);
    builder.containerInstanceKey(generateInstanceKeyForContainer(containerInfo));
    builder.instanceInfo(containerInfo);

    return builder.build();
  }

  private Instance buildInstanceFromPodInfo(
      InfrastructureMapping infraMapping, K8sPod pod, DeploymentSummary deploymentSummary) {
    InstanceBuilder builder = buildInstanceBase(null, infraMapping, deploymentSummary);
    builder.podInstanceKey(PodInstanceKey.builder().podName(pod.getName()).namespace(pod.getNamespace()).build());
    builder.instanceInfo(K8sPodInfo.builder()
                             .releaseName(pod.getReleaseName())
                             .podName(pod.getName())
                             .ip(pod.getPodIP())
                             .namespace(pod.getNamespace())
                             .containers(pod.getContainerList()
                                             .stream()
                                             .map(container
                                                 -> K8sContainerInfo.builder()
                                                        .containerId(container.getContainerId())
                                                        .name(container.getName())
                                                        .image(container.getImage())
                                                        .build())
                                             .collect(toList()))
                             .blueGreenColor(pod.getColor())
                             .build());

    boolean instanceBuilderUpdated = false;
    if (deploymentSummary != null && deploymentSummary.getArtifactStreamId() != null) {
      for (K8sContainer k8sContainer : pod.getContainerList()) {
        Artifact artifact = wingsPersistence.createQuery(Artifact.class)
                                .filter(ArtifactKeys.artifactStreamId, deploymentSummary.getArtifactStreamId())
                                .filter(ArtifactKeys.appId, infraMapping.getAppId())
                                .filter("metadata.image", k8sContainer.getImage())
                                .disableValidation()
                                .get();
        if (artifact != null) {
          builder.lastArtifactId(artifact.getUuid());
          updateInstanceWithArtifactSourceAndBuildNum(builder, k8sContainer);
          instanceBuilderUpdated = true;
          break;
        }
      }
    }

    if (!instanceBuilderUpdated) {
      updateInstanceWithArtifactSourceAndBuildNum(builder, pod.getContainerList().get(0));
    }

    return builder.build();
  }

  private void updateInstanceWithArtifactSourceAndBuildNum(InstanceBuilder builder, K8sContainer container) {
    String image = container.getImage();
    String artifactSource;
    String tag;
    String[] splitArray = image.split(":");
    if (splitArray.length == 2) {
      artifactSource = splitArray[0];
      tag = splitArray[1];
    } else if (splitArray.length == 1) {
      artifactSource = splitArray[0];
      tag = "latest";
    } else {
      artifactSource = image;
      tag = image;
    }

    builder.lastArtifactName(container.getImage());
    builder.lastArtifactSourceName(artifactSource);
    builder.lastArtifactBuildNum(tag);
  }

  private ContainerInstanceKey generateInstanceKeyForContainer(ContainerInfo containerInfo) {
    ContainerInstanceKey containerInstanceKey;

    if (containerInfo instanceof KubernetesContainerInfo) {
      KubernetesContainerInfo kubernetesContainerInfo = (KubernetesContainerInfo) containerInfo;
      containerInstanceKey = ContainerInstanceKey.builder()
                                 .containerId(kubernetesContainerInfo.getPodName())
                                 .namespace(((KubernetesContainerInfo) containerInfo).getNamespace())
                                 .build();
    } else if (containerInfo instanceof EcsContainerInfo) {
      EcsContainerInfo ecsContainerInfo = (EcsContainerInfo) containerInfo;
      containerInstanceKey = ContainerInstanceKey.builder().containerId(ecsContainerInfo.getTaskArn()).build();
    } else {
      String msg = "Unsupported container instance type:" + containerInfo;
      log.error(msg);
      throw new GeneralException(msg);
    }

    return containerInstanceKey;
  }

  private String getContainerSvcName(ContainerInfo containerInfo) {
    if (containerInfo instanceof KubernetesContainerInfo) {
      return ((KubernetesContainerInfo) containerInfo).getControllerName();
    } else if (containerInfo instanceof EcsContainerInfo) {
      return ((EcsContainerInfo) containerInfo).getServiceName();
    }
    if (containerInfo instanceof K8sPodInfo) {
      return null;
    } else {
      throw new GeneralException(
          "Unsupported container deploymentInfo type:" + containerInfo.getClass().getCanonicalName());
    }
  }

  private ContainerSyncResponse getLatestInstancesFromContainerServer(
      Collection<software.wings.beans.infrastructure.instance.ContainerDeploymentInfo>
          containerDeploymentInfoCollection,
      InstanceType instanceType) {
    ContainerFilter containerFilter =
        ContainerFilter.builder().containerDeploymentInfoCollection(containerDeploymentInfoCollection).build();

    ContainerSyncRequest instanceSyncRequest = ContainerSyncRequest.builder().filter(containerFilter).build();
    if (instanceType == InstanceType.KUBERNETES_CONTAINER_INSTANCE
        || instanceType == InstanceType.ECS_CONTAINER_INSTANCE) {
      return containerSync.getInstances(instanceSyncRequest);
    } else {
      String msg = "Unsupported container instance type:" + instanceType;
      log.error(msg);
      throw new GeneralException(msg);
    }
  }

  public Set<ContainerMetadata> getContainerServiceNames(
      ExecutionContext context, String serviceId, String infraMappingId, Optional<String> infrastructureDefinitionId) {
    Set<ContainerMetadata> containerMetadataSet = Sets.newHashSet();
    List<StateExecutionInstance> executionDataList =
        workflowExecutionService.getStateExecutionData(context.getAppId(), context.getWorkflowExecutionId(), serviceId,
            infraMappingId, infrastructureDefinitionId, StateType.PHASE_STEP, WorkflowServiceHelper.DEPLOY_CONTAINERS);
    executionDataList.forEach(stateExecutionData -> {
      List<StateExecutionData> deployPhaseStepList =
          stateExecutionData.getStateExecutionMap()
              .entrySet()
              .stream()
              .filter(entry -> entry.getKey().equals(WorkflowServiceHelper.DEPLOY_CONTAINERS))
              .map(Entry::getValue)
              .collect(toList());
      deployPhaseStepList.forEach(phaseStep -> {
        PhaseStepExecutionSummary phaseStepExecutionSummary =
            ((PhaseStepExecutionData) phaseStep).getPhaseStepExecutionSummary();
        Preconditions.checkNotNull(
            phaseStepExecutionSummary, "PhaseStepExecutionSummary is null for stateExecutionInstanceId: " + phaseStep);
        List<StepExecutionSummary> stepExecutionSummaryList = phaseStepExecutionSummary.getStepExecutionSummaryList();
        Preconditions.checkNotNull(
            stepExecutionSummaryList, "stepExecutionSummaryList null for " + phaseStepExecutionSummary);

        for (StepExecutionSummary stepExecutionSummary : stepExecutionSummaryList) {
          if (stepExecutionSummary instanceof CommandStepExecutionSummary) {
            CommandStepExecutionSummary commandStepExecutionSummary =
                (CommandStepExecutionSummary) stepExecutionSummary;
            if (commandStepExecutionSummary.getOldInstanceData() != null) {
              containerMetadataSet.addAll(commandStepExecutionSummary.getOldInstanceData()
                                              .stream()
                                              .map(containerServiceData
                                                  -> ContainerMetadata.builder()
                                                         .containerServiceName(containerServiceData.getName())
                                                         .namespace(commandStepExecutionSummary.getNamespace())
                                                         .build())
                                              .collect(toList()));
            }

            if (commandStepExecutionSummary.getNewInstanceData() != null) {
              containerMetadataSet.addAll(commandStepExecutionSummary.getNewInstanceData()
                                              .stream()
                                              .map(containerServiceData
                                                  -> ContainerMetadata.builder()
                                                         .containerServiceName(containerServiceData.getName())
                                                         .namespace(commandStepExecutionSummary.getNamespace())
                                                         .build())
                                              .collect(toList()));
            }

            Preconditions.checkState(!containerMetadataSet.isEmpty(),
                "Both old and new container services are empty. Cannot proceed for phase step "
                    + commandStepExecutionSummary.getServiceId());
          }
        }
      });
    });

    return containerMetadataSet;
  }

  public List<ContainerInfo> getContainerInfoForService(Set<ContainerMetadata> containerMetadataSet,
      ExecutionContext context, String infrastructureMappingId, String serviceId) {
    Preconditions.checkState(!containerMetadataSet.isEmpty(), "empty for " + context.getWorkflowExecutionId());
    InfrastructureMapping infrastructureMapping = infraMappingService.get(context.getAppId(), infrastructureMappingId);
    InstanceType instanceType = instanceUtil.getInstanceType(infrastructureMapping.getInfraMappingType());
    Preconditions.checkNotNull(instanceType, "Null for " + infrastructureMappingId);

    String containerSvcNameNoRevision =
        getcontainerSvcNameNoRevision(instanceType, containerMetadataSet.iterator().next().getContainerServiceName());
    Map<String, ContainerDeploymentInfo> containerSvcNameDeploymentInfoMap =
        instanceService.getContainerDeploymentInfoList(containerSvcNameNoRevision, context.getAppId())
            .stream()
            .collect(toMap(ContainerDeploymentInfo::getContainerSvcName, identity()));

    for (ContainerMetadata containerMetadata : containerMetadataSet) {
      ContainerDeploymentInfo containerDeploymentInfo =
          containerSvcNameDeploymentInfoMap.get(containerMetadata.getContainerServiceName());
      if (containerDeploymentInfo == null) {
        containerDeploymentInfo = ContainerDeploymentInfo.builder()
                                      .appId(context.getAppId())
                                      .containerSvcName(containerMetadata.getContainerServiceName())
                                      .infraMappingId(infrastructureMappingId)
                                      .workflowId(context.getWorkflowId())
                                      .workflowExecutionId(context.getWorkflowExecutionId())
                                      .serviceId(serviceId)
                                      .namespace(containerMetadata.getNamespace())
                                      .build();

        containerSvcNameDeploymentInfoMap.put(containerMetadata.getContainerServiceName(), containerDeploymentInfo);
      }
    }
    ContainerSyncResponse instanceSyncResponse =
        getLatestInstancesFromContainerServer(containerSvcNameDeploymentInfoMap.values(), instanceType);
    Preconditions.checkNotNull(instanceSyncResponse, "InstanceSyncResponse");

    return instanceSyncResponse.getContainerInfoList();
  }

  private String getcontainerSvcNameNoRevision(InstanceType instanceType, String containerSvcName) {
    String delimiter;
    if (instanceType == InstanceType.ECS_CONTAINER_INSTANCE) {
      delimiter = "__";
    } else if (instanceType == InstanceType.KUBERNETES_CONTAINER_INSTANCE) {
      delimiter = "-";
    } else {
      String msg = "Unsupported container instance type:" + instanceType;
      log.error(msg);
      throw new GeneralException(msg);
    }

    if (containerSvcName == null) {
      return null;
    }

    int index = containerSvcName.lastIndexOf(delimiter);
    if (index == -1) {
      return containerSvcName;
    }
    return containerSvcName.substring(0, index);
  }

  @Override
  public DeploymentKey generateDeploymentKey(DeploymentInfo deploymentInfo) {
    if (deploymentInfo instanceof ContainerDeploymentInfoWithNames) {
      ContainerDeploymentInfoWithNames deploymentInfoWithNames = (ContainerDeploymentInfoWithNames) deploymentInfo;
      String keyName = isNotEmpty(deploymentInfoWithNames.getUniqueNameIdentifier())
          ? deploymentInfoWithNames.getUniqueNameIdentifier()
          : deploymentInfoWithNames.getContainerSvcName();

      return ContainerDeploymentKey.builder().containerServiceName(keyName).build();
    } else if (deploymentInfo instanceof ContainerDeploymentInfoWithLabels) {
      ContainerDeploymentInfoWithLabels info = (ContainerDeploymentInfoWithLabels) deploymentInfo;
      ContainerDeploymentKey key = ContainerDeploymentKey.builder().labels(info.getLabels()).build();

      // For Helm
      if (EmptyPredicate.isNotEmpty(info.getNewVersion())) {
        key.setNewVersion(info.getNewVersion());
      }
      return key;
    } else if (deploymentInfo instanceof K8sDeploymentInfo) {
      K8sDeploymentInfo k8sDeploymentInfo = (K8sDeploymentInfo) deploymentInfo;
      return K8sDeploymentKey.builder()
          .releaseName(k8sDeploymentInfo.getReleaseName())
          .releaseNumber(k8sDeploymentInfo.getReleaseNumber())
          .build();
    } else {
      throw new GeneralException("Unsupported DeploymentInfo type for Container: " + deploymentInfo);
    }
  }

  @Override
  protected void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey) {
    if (deploymentKey instanceof ContainerDeploymentKey) {
      deploymentSummary.setContainerDeploymentKey((ContainerDeploymentKey) deploymentKey);
    } else if (deploymentKey instanceof K8sDeploymentKey) {
      deploymentSummary.setK8sDeploymentKey((K8sDeploymentKey) deploymentKey);
    } else {
      throw new GeneralException("Invalid deploymentKey passed for ContainerDeploymentKey" + deploymentKey);
    }
  }

  @Override
  public FeatureName getFeatureFlagToEnablePerpetualTaskForInstanceSync() {
    return FeatureName.MOVE_CONTAINER_INSTANCE_SYNC_TO_PERPETUAL_TASK;
  }

  @Override
  public InstanceSyncPerpetualTaskCreator getInstanceSyncPerpetualTaskCreator() {
    return taskCreator;
  }

  @Override
  public void processInstanceSyncResponseFromPerpetualTask(
      InfrastructureMapping infrastructureMapping, DelegateResponseData response) {
    if (!(infrastructureMapping instanceof ContainerInfrastructureMapping)) {
      String msg = "Incompatible infrastructure mapping type found:" + infrastructureMapping.getInfraMappingType();
      log.error(msg);
      throw new GeneralException(msg);
    }

    ContainerInfrastructureMapping containerInfrastructureMapping =
        (ContainerInfrastructureMapping) infrastructureMapping;
    syncInstancesInternal(
        containerInfrastructureMapping, ArrayListMultimap.create(), null, false, response, PERPETUAL_TASK);
  }

  @Override
  public Status getStatus(InfrastructureMapping infrastructureMapping, DelegateResponseData response) {
    if (!(response instanceof ContainerSyncResponse) && !(response instanceof K8sTaskExecutionResponse)) {
      throw new GeneralException("Incompatible response data received from perpetual task execution");
    }

    return response instanceof K8sTaskExecutionResponse
        ? getK8sPerpetualTaskStatus((K8sTaskExecutionResponse) response)
        : getContainerSyncPerpetualTaskStatus((ContainerSyncResponse) response);
  }

  private Status getK8sPerpetualTaskStatus(K8sTaskExecutionResponse response) {
    boolean success = response.getCommandExecutionStatus() == SUCCESS;
    K8sInstanceSyncResponse k8sInstanceSyncResponse = (K8sInstanceSyncResponse) response.getK8sTaskResponse();
    boolean deleteTask = success && isEmpty(k8sInstanceSyncResponse.getK8sPodInfoList());
    String errorMessage = success ? null : response.getErrorMessage();

    return Status.builder().retryable(!deleteTask).errorMessage(errorMessage).success(success).build();
  }

  private Status getContainerSyncPerpetualTaskStatus(ContainerSyncResponse response) {
    boolean success = response.getCommandExecutionStatus() == SUCCESS;
    boolean deleteTask = success && isEmpty(response.getContainerInfoList());
    String errorMessage = success ? null : response.getErrorMessage();

    return Status.builder().retryable(!deleteTask).errorMessage(errorMessage).success(success).build();
  }
}
