package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.container.Label.Builder.aLabel;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import software.wings.api.CommandStepExecutionSummary;
import software.wings.api.ContainerDeploymentInfoWithLabels;
import software.wings.api.ContainerDeploymentInfoWithNames;
import software.wings.api.ContainerServiceData;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.HelmSetupExecutionSummary;
import software.wings.api.KubernetesSteadyStateCheckExecutionSummary;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.container.Label;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.common.Constants;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.instance.sync.ContainerSync;
import software.wings.service.impl.instance.sync.request.ContainerFilter;
import software.wings.service.impl.instance.sync.request.ContainerSyncRequest;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.sm.ExecutionContext;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.StepExecutionSummary;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author rktummala on 02/03/18
 */
@Singleton
public class ContainerInstanceHandler extends InstanceHandler {
  @Inject private ContainerSync containerSync;

  @Override
  public void syncInstances(String appId, String infraMappingId) throws HarnessException {
    // Key - containerSvcName, Value - Instances
    Multimap<String, Instance> containerSvcNameInstanceMap = ArrayListMultimap.create();
    syncInstancesInternal(appId, infraMappingId, containerSvcNameInstanceMap, null, false);
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
   * @param appId
   * @param infraMappingId
   * @param containerSvcNameInstanceMap key - containerSvcName     value - Instances
   * @throws HarnessException
   */
  protected void syncInstancesInternal(String appId, String infraMappingId,
      Multimap<String, Instance> containerSvcNameInstanceMap, List<DeploymentSummary> newDeploymentSummaries,
      boolean rollback) throws HarnessException {
    ContainerInfrastructureMapping containerInfraMapping = getContainerInfraMapping(appId, infraMappingId);

    Map<String, DeploymentSummary> serviceNamesNewDeploymentSummaryMap =
        getDeploymentSummaryMap(newDeploymentSummaries, containerSvcNameInstanceMap, containerInfraMapping);

    loadContainerSvcNameInstanceMap(appId, infraMappingId, containerSvcNameInstanceMap);

    // This is to handle the case of the instances stored in the new schema.
    if (containerSvcNameInstanceMap.size() > 0) {
      containerSvcNameInstanceMap.keySet().forEach(containerSvcName -> {
        // Get all the instances for the given containerSvcName (In kubernetes, this is replication Controller and in
        // ECS it is taskDefinition)
        ContainerSyncResponse instanceSyncResponse =
            containerSync.getInstances(containerInfraMapping, asList(containerSvcName));
        Validator.notNullCheck("InstanceSyncResponse is null for containerSvcName: " + containerSvcName
                + " for infraMappingId: " + infraMappingId,
            instanceSyncResponse);

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

        // Find the instances that were yet to be added to db
        SetView<String> instancesToBeAdded =
            Sets.difference(latestContainerInfoMap.keySet(), instancesInDBMap.keySet());

        SetView<String> instancesToBeDeleted =
            Sets.difference(instancesInDBMap.keySet(), latestContainerInfoMap.keySet());

        Set<String> instanceIdsToBeDeleted = new HashSet<>();
        instancesToBeDeleted.stream().forEach(ec2InstanceId -> {
          Instance instance = instancesInDBMap.get(ec2InstanceId);
          if (instance != null) {
            instanceIdsToBeDeleted.add(instance.getUuid());
          }
        });

        long instanceInDBCount = instancesInDB.stream().filter(instance -> instance != null).count();
        logger.info(
            "Total no of Container instances found in DB for ContainerSvcName: {} and InfraMappingId: {} and AppId: {}, "
                + "No of instances in DB: {}, No of Running instances: {}, "
                + "No of instances to be Added: {}, No of instances to be deleted: {}",
            containerSvcName, infraMappingId, appId, instanceInDBCount, latestContainerInfoMap.keySet().size(),
            instancesToBeAdded.size(), instanceIdsToBeDeleted.size());
        if (isNotEmpty(instanceIdsToBeDeleted)) {
          instanceService.delete(instanceIdsToBeDeleted);
        }

        DeploymentSummary deploymentSummary;
        if (isNotEmpty(instancesToBeAdded)) {
          // newDeploymentInfo would be null in case of sync job.
          if ((containerSvcNameInstanceMap == null
                  || !serviceNamesNewDeploymentSummaryMap.containsKey(containerSvcName))
              && isNotEmpty(instancesInDB)) {
            Optional<Instance> instanceWithExecutionInfoOptional = getInstanceWithExecutionInfo(instancesInDB);
            if (!instanceWithExecutionInfoOptional.isPresent()) {
              logger.warn("Couldn't find an instance from a previous deployment for inframapping {}", infraMappingId);
              return;
            }

            DeploymentSummary deploymentSummaryFromPrevious =
                DeploymentSummary.builder().deploymentInfo(ContainerDeploymentInfoWithNames.builder().build()).build();
            // We pick one of the existing instances from db for the same controller / task definition
            generateDeploymentSummaryFromInstance(
                instanceWithExecutionInfoOptional.get(), deploymentSummaryFromPrevious);
            deploymentSummary = deploymentSummaryFromPrevious;
          } else {
            deploymentSummary = getDeploymentSummaryForInstanceCreation(
                serviceNamesNewDeploymentSummaryMap.get(containerSvcName), rollback);
          }

          instancesToBeAdded.forEach(containerId -> {
            ContainerInfo containerInfo = latestContainerInfoMap.get(containerId);
            Instance instance = buildInstanceFromContainerInfo(containerInfraMapping, containerInfo, deploymentSummary);
            instanceService.saveOrUpdate(instance);
          });
        }
      });
    }
  }

  private Map<String, DeploymentSummary> getDeploymentSummaryMap(List<DeploymentSummary> newDeploymentSummaries,
      Multimap<String, Instance> containerSvcNameInstanceMap, ContainerInfrastructureMapping containerInfraMapping) {
    if (EmptyPredicate.isEmpty(newDeploymentSummaries)) {
      return Collections.EMPTY_MAP;
    }

    Map<String, DeploymentSummary> deploymentSummaryMap = new HashMap<>();

    if (newDeploymentSummaries.stream().iterator().next().getDeploymentInfo()
            instanceof ContainerDeploymentInfoWithLabels) {
      Map<String, String> labelMap = new HashMap<>();
      for (DeploymentSummary deploymentSummary : newDeploymentSummaries) {
        ((ContainerDeploymentInfoWithLabels) deploymentSummary.getDeploymentInfo())
            .getLabels()
            .stream()
            .forEach(labelEntry -> labelMap.put(labelEntry.getName(), labelEntry.getValue()));

        Set<String> controllerNames = containerSync.getControllerNames(containerInfraMapping, labelMap);

        logger.info(
            "Number of controllers returned for executionId [{}], inframappingId [{}], appId [{}] from labels: {}",
            newDeploymentSummaries.iterator().next().getWorkflowExecutionId(), containerInfraMapping.getUuid(),
            newDeploymentSummaries.iterator().next().getAppId(), controllerNames.size());

        controllerNames.stream().forEach(controllerName -> {
          deploymentSummaryMap.put(controllerName, deploymentSummary);
          containerSvcNameInstanceMap.put(controllerName, null);
        });
      }
    } else {
      newDeploymentSummaries.stream().forEach(deploymentSummary
          -> deploymentSummaryMap.put(

              ((ContainerDeploymentInfoWithNames) deploymentSummary.getDeploymentInfo()).getContainerSvcName(),
              deploymentSummary));
    }

    return deploymentSummaryMap;
  }
  private void loadContainerSvcNameInstanceMap(String appId, String infraMappingId,
      Multimap<String, Instance> containerSvcNameInstanceMap) throws HarnessException {
    List<Instance> instanceListInDBForInfraMapping = getInstances(appId, infraMappingId);
    for (Instance instance : instanceListInDBForInfraMapping) {
      InstanceInfo instanceInfo = instance.getInstanceInfo();
      if (instanceInfo instanceof ContainerInfo) {
        ContainerInfo containerInfo = (ContainerInfo) instanceInfo;
        String containerSvcName = getContainerSvcName(containerInfo);
        containerSvcNameInstanceMap.put(containerSvcName, instance);
      } else {
        throw new HarnessException("UnSupported instance deploymentInfo type" + instance.getInstanceType().name());
      }
    }
  }

  @Override
  public void handleNewDeployment(List<DeploymentSummary> deploymentSummaries, boolean rollback)
      throws HarnessException {
    Multimap<String, Instance> containerSvcNameInstanceMap = ArrayListMultimap.create();

    if (isEmpty(deploymentSummaries)) {
      return;
    }

    String infraMappingId = deploymentSummaries.iterator().next().getInfraMappingId();
    String appId = deploymentSummaries.iterator().next().getAppId();
    String workflowExecutionId = deploymentSummaries.iterator().next().getWorkflowExecutionId();
    logger.info("Handling new container deployment for executionId [{}], inframappingId [{}], appId [{}]",
        workflowExecutionId, infraMappingId, appId);
    validateDeploymentInfos(deploymentSummaries);

    if (deploymentSummaries.iterator().next().getDeploymentInfo() instanceof ContainerDeploymentInfoWithNames) {
      deploymentSummaries.stream().forEach(deploymentSummary -> {
        ContainerDeploymentInfoWithNames containerDeploymentInfo =
            (ContainerDeploymentInfoWithNames) deploymentSummary.getDeploymentInfo();
        containerSvcNameInstanceMap.put(containerDeploymentInfo.getContainerSvcName(), null);
      });
    }

    syncInstancesInternal(appId, infraMappingId, containerSvcNameInstanceMap, deploymentSummaries, rollback);
  }

  private void validateDeploymentInfos(List<DeploymentSummary> deploymentSummaries) throws HarnessException {
    for (DeploymentSummary deploymentSummary : deploymentSummaries) {
      DeploymentInfo deploymentInfo = deploymentSummary.getDeploymentInfo();
      if (!(deploymentInfo instanceof ContainerDeploymentInfoWithNames)
          && !(deploymentInfo instanceof ContainerDeploymentInfoWithLabels)) {
        throw new HarnessException("Incompatible deployment info type: " + deploymentInfo);
      }
    }
  }

  public boolean isContainerDeployment(InfrastructureMapping infrastructureMapping) {
    return infrastructureMapping instanceof ContainerInfrastructureMapping;
  }

  @Override
  public Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
      InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact)
      throws HarnessException {
    PhaseStepExecutionSummary phaseStepExecutionSummary = phaseStepExecutionData.getPhaseStepExecutionSummary();

    if (phaseStepExecutionSummary == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("PhaseStepExecutionSummary is null for stateExecutionInstanceId: " + stateExecutionInstanceId);
      }
      return Optional.empty();
    }
    List<StepExecutionSummary> stepExecutionSummaryList = phaseStepExecutionSummary.getStepExecutionSummaryList();
    // This was observed when the "deploy containers" step was executed in rollback and no commands were
    // executed since setup failed.
    if (stepExecutionSummaryList == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("StepExecutionSummaryList is null for stateExecutionInstanceId: " + stateExecutionInstanceId);
      }
      return Optional.empty();
    }

    for (StepExecutionSummary stepExecutionSummary : stepExecutionSummaryList) {
      if (stepExecutionSummary != null) {
        if (stepExecutionSummary instanceof CommandStepExecutionSummary) {
          CommandStepExecutionSummary commandStepExecutionSummary = (CommandStepExecutionSummary) stepExecutionSummary;
          String clusterName = commandStepExecutionSummary.getClusterName();
          Set<String> containerSvcNameSet = Sets.newHashSet();

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

          if (containerSvcNameSet.isEmpty()) {
            logger.warn(
                "Both old and new container services are empty. Cannot proceed for phase step for state execution instance: {}",
                stateExecutionInstanceId);
            return Optional.empty();
          }

          List<DeploymentInfo> containerDeploymentInfoWithNames =
              getContainerDeploymentInfos(clusterName, containerSvcNameSet);

          return Optional.of(containerDeploymentInfoWithNames);

        } else if (stepExecutionSummary instanceof HelmSetupExecutionSummary
            || stepExecutionSummary instanceof KubernetesSteadyStateCheckExecutionSummary) {
          if (!(infrastructureMapping instanceof ContainerInfrastructureMapping)) {
            logger.warn("Inframapping is not container type. cannot proceed for state execution instance: {}",
                stateExecutionInstanceId);
            return Optional.empty();
          }

          String clusterName = ((ContainerInfrastructureMapping) infrastructureMapping).getClusterName();

          List<Label> labels = new ArrayList();

          if (stepExecutionSummary instanceof HelmSetupExecutionSummary) {
            HelmSetupExecutionSummary helmSetupExecutionSummary = (HelmSetupExecutionSummary) stepExecutionSummary;
            labels.add(aLabel().withName("release").withValue(helmSetupExecutionSummary.getReleaseName()).build());
          } else {
            KubernetesSteadyStateCheckExecutionSummary kubernetesSteadyStateCheckExecutionSummary =
                (KubernetesSteadyStateCheckExecutionSummary) stepExecutionSummary;
            labels.addAll(kubernetesSteadyStateCheckExecutionSummary.getLabels());
          }

          ContainerInfrastructureMapping containerInfraMapping = (ContainerInfrastructureMapping) infrastructureMapping;
          return Optional.of(Arrays.asList(getContainerDeploymentInfosWithLables(clusterName, labels)));
        }
      }
    }
    return Optional.empty();
  }

  private DeploymentInfo getContainerDeploymentInfosWithLables(String clusterName, List<Label> labels) {
    return ContainerDeploymentInfoWithLabels.builder().clusterName(clusterName).labels(labels).build();
  }

  private List<DeploymentInfo> getContainerDeploymentInfos(String clusterName, Set<String> containerSvcNameSet) {
    List<DeploymentInfo> containerDeploymentInfoWithNames = new ArrayList<>();
    containerSvcNameSet.stream().forEach(containerSvcName
        -> containerDeploymentInfoWithNames.add(ContainerDeploymentInfoWithNames.builder()
                                                    .containerSvcName(containerSvcName)
                                                    .clusterName(clusterName)
                                                    .build()));
    return containerDeploymentInfoWithNames;
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

  public Instance buildInstanceFromContainerInfo(
      InfrastructureMapping infraMapping, ContainerInfo containerInfo, DeploymentSummary deploymentSummary) {
    InstanceBuilder builder = buildInstanceBase(null, infraMapping, deploymentSummary);
    builder.containerInstanceKey(generateInstanceKeyForContainer(containerInfo));
    builder.instanceInfo(containerInfo);

    return builder.build();
  }

  private ContainerInstanceKey generateInstanceKeyForContainer(ContainerInfo containerInfo) {
    ContainerInstanceKey containerInstanceKey;

    if (containerInfo instanceof KubernetesContainerInfo) {
      KubernetesContainerInfo kubernetesContainerInfo = (KubernetesContainerInfo) containerInfo;
      containerInstanceKey = ContainerInstanceKey.builder().containerId(kubernetesContainerInfo.getPodName()).build();
    } else if (containerInfo instanceof EcsContainerInfo) {
      EcsContainerInfo ecsContainerInfo = (EcsContainerInfo) containerInfo;
      containerInstanceKey = ContainerInstanceKey.builder().containerId(ecsContainerInfo.getTaskArn()).build();
    } else {
      String msg = "Unsupported container instance type:" + containerInfo;
      logger.error(msg);
      throw new WingsException(msg);
    }

    return containerInstanceKey;
  }

  public String getContainerSvcName(ContainerInfo containerInfo) {
    if (containerInfo instanceof KubernetesContainerInfo) {
      return ((KubernetesContainerInfo) containerInfo).getControllerName();
    } else if (containerInfo instanceof EcsContainerInfo) {
      return ((EcsContainerInfo) containerInfo).getServiceName();
    } else {
      throw new WingsException(
          "Unsupported container deploymentInfo type:" + containerInfo.getClass().getCanonicalName());
    }
  }

  public ContainerSyncResponse getLatestInstancesFromContainerServer(
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
      logger.error(msg);
      throw new WingsException(msg);
    }
  }

  public Set<String> getContainerServiceNames(ExecutionContext context, String serviceId, String infraMappingId) {
    Set<String> containerSvcNameSet = Sets.newHashSet();
    List<StateExecutionInstance> executionDataList = workflowExecutionService.getStateExecutionData(context.getAppId(),
        context.getWorkflowExecutionId(), serviceId, infraMappingId, StateType.PHASE_STEP, Constants.DEPLOY_CONTAINERS);
    executionDataList.forEach(stateExecutionData -> {
      List<StateExecutionData> deployPhaseStepList =
          stateExecutionData.getStateExecutionMap()
              .entrySet()
              .stream()
              .filter(entry -> entry.getKey().equals(Constants.DEPLOY_CONTAINERS))
              .map(entry -> entry.getValue())
              .collect(toList());
      deployPhaseStepList.stream().forEach(phaseStep -> {
        PhaseStepExecutionSummary phaseStepExecutionSummary =
            ((PhaseStepExecutionData) phaseStep).getPhaseStepExecutionSummary();
        Preconditions.checkNotNull(
            phaseStepExecutionSummary, "PhaseStepExecutionSummary is null for stateExecutionInstanceId: " + phaseStep);
        List<StepExecutionSummary> stepExecutionSummaryList = phaseStepExecutionSummary.getStepExecutionSummaryList();
        Preconditions.checkNotNull(
            stepExecutionSummaryList, "stepExecutionSummaryList null for " + phaseStepExecutionSummary);

        for (StepExecutionSummary stepExecutionSummary : stepExecutionSummaryList) {
          if (stepExecutionSummary != null && stepExecutionSummary instanceof CommandStepExecutionSummary) {
            CommandStepExecutionSummary commandStepExecutionSummary =
                (CommandStepExecutionSummary) stepExecutionSummary;
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

            Preconditions.checkState(!containerSvcNameSet.isEmpty(),
                "Both old and new container services are empty. Cannot proceed for phase step "
                    + commandStepExecutionSummary.getServiceId());
          }
        }
      });
    });

    return containerSvcNameSet;
  }

  public List<ContainerInfo> getContainerInfoForService(
      Set<String> containerSvcNames, ExecutionContext context, String infrastructureMappingId, String serviceId) {
    Preconditions.checkState(!containerSvcNames.isEmpty(), "empty for " + context);
    InfrastructureMapping infrastructureMapping = infraMappingService.get(context.getAppId(), infrastructureMappingId);
    InstanceType instanceType = instanceUtil.getInstanceType(infrastructureMapping.getInfraMappingType());
    Preconditions.checkNotNull(instanceType, "Null for " + infrastructureMappingId);

    String containerSvcNameNoRevision =
        getcontainerSvcNameNoRevision(instanceType, containerSvcNames.iterator().next());
    Map<String, software.wings.beans.infrastructure.instance.ContainerDeploymentInfo>
        containerSvcNameDeploymentInfoMap = Maps.newHashMap();

    List<software.wings.beans.infrastructure.instance.ContainerDeploymentInfo> currentContainerDeploymentsInDB =
        instanceService.getContainerDeploymentInfoList(containerSvcNameNoRevision, context.getAppId());

    currentContainerDeploymentsInDB.stream().forEach(currentContainerDeploymentInDB
        -> containerSvcNameDeploymentInfoMap.put(
            currentContainerDeploymentInDB.getContainerSvcName(), currentContainerDeploymentInDB));

    for (String containerSvcName : containerSvcNames) {
      software.wings.beans.infrastructure.instance.ContainerDeploymentInfo containerDeploymentInfo =
          containerSvcNameDeploymentInfoMap.get(containerSvcName);
      if (containerDeploymentInfo == null) {
        containerDeploymentInfo =
            software.wings.beans.infrastructure.instance.ContainerDeploymentInfo.Builder.aContainerDeploymentInfo()
                .withAppId(context.getAppId())
                .withContainerSvcName(containerSvcName)
                .withInfraMappingId(infrastructureMappingId)
                .withWorkflowId(context.getWorkflowId())
                .withWorkflowExecutionId(context.getWorkflowExecutionId())
                .withServiceId(serviceId)
                .build();

        containerSvcNameDeploymentInfoMap.put(containerSvcName, containerDeploymentInfo);
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
      delimiter = ".";
    } else {
      String msg = "Unsupported container instance type:" + instanceType;
      logger.error(msg);
      throw new WingsException(msg);
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
      return ContainerDeploymentKey.builder()
          .containerServiceName(((ContainerDeploymentInfoWithNames) deploymentInfo).getContainerSvcName())
          .build();
    } else if (deploymentInfo instanceof ContainerDeploymentInfoWithLabels) {
      return ContainerDeploymentKey.builder()
          .labels(((ContainerDeploymentInfoWithLabels) deploymentInfo).getLabels())
          .build();
    } else {
      throw new WingsException("Unsupported DeploymentINfo type for Container: " + deploymentInfo);
    }
  }

  @Override
  protected void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey) {
    if (deploymentKey instanceof ContainerDeploymentKey) {
      deploymentSummary.setContainerDeploymentKey((ContainerDeploymentKey) deploymentKey);
    } else {
      throw new WingsException("Invalid deploymentKey passed for ContainerDeploymentKey" + deploymentKey);
    }
  }
}
