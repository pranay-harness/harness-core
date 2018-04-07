package software.wings.service.impl.instance;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.hazelcast.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CommandStepExecutionSummary;
import software.wings.api.ContainerDeploymentInfo;
import software.wings.api.ContainerServiceData;
import software.wings.api.DeploymentEvent;
import software.wings.api.DeploymentInfo;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.common.Constants;
import software.wings.core.queue.Queue;
import software.wings.exception.WingsException;
import software.wings.service.impl.instance.sync.ContainerSync;
import software.wings.service.impl.instance.sync.request.ContainerFilter;
import software.wings.service.impl.instance.sync.request.ContainerSyncRequest;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.StepExecutionSummary;
import software.wings.utils.Validator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Handles all the container instance related operations like querying the latest instances from container provider,
 * building instances out of container deploymentInfo, etc.
 *
 * @author rktummala on 09/10/17
 */
public class ContainerInstanceHelper {
  private static final Logger logger = LoggerFactory.getLogger(ContainerInstanceHelper.class);
  @Inject private AppService appService;
  @Inject private InstanceUtil instanceUtil;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private InstanceHelper instanceHelper;
  @Inject private InstanceService instanceService;

  @Inject private ContainerSync containerSync;

  // This queue is used to asynchronously process all the container deployment information that the workflow touched
  // upon.
  @Inject private Queue<DeploymentEvent> deploymentEventQueue;

  public boolean isContainerDeployment(InfrastructureMapping infrastructureMapping) {
    return infrastructureMapping instanceof ContainerInfrastructureMapping
        || infrastructureMapping instanceof GcpKubernetesInfrastructureMapping
        || infrastructureMapping instanceof AzureKubernetesInfrastructureMapping
        || infrastructureMapping instanceof DirectKubernetesInfrastructureMapping;
  }

  public Optional<DeploymentEvent> extractContainerInfoAndSendEvent(String stateExecutionInstanceId,
      PhaseExecutionData phaseExecutionData, WorkflowExecution workflowExecution, Artifact artifact,
      InfrastructureMapping infrastructureMapping) {
    PhaseExecutionSummary phaseExecutionSummary = phaseExecutionData.getPhaseExecutionSummary();
    if (phaseExecutionSummary != null) {
      Map<String, PhaseStepExecutionSummary> phaseStepExecutionSummaryMap =
          phaseExecutionSummary.getPhaseStepExecutionSummaryMap();
      if (phaseStepExecutionSummaryMap != null) {
        PhaseStepExecutionSummary phaseStepExecutionSummary =
            phaseStepExecutionSummaryMap.get(Constants.DEPLOY_CONTAINERS);
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
          if (stepExecutionSummary != null && stepExecutionSummary instanceof CommandStepExecutionSummary) {
            CommandStepExecutionSummary commandStepExecutionSummary =
                (CommandStepExecutionSummary) stepExecutionSummary;
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

            return Optional.of(buildContainerDeploymentEvent(stateExecutionInstanceId, workflowExecution,
                phaseExecutionData, clusterName, containerSvcNameSet, infrastructureMapping, artifact));
          }
        }
      }
    }

    return Optional.empty();
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
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(context.getAppId(), infrastructureMappingId);
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

  private DeploymentEvent buildContainerDeploymentEvent(String stateExecutionInstanceId,
      WorkflowExecution workflowExecution, PhaseExecutionData phaseExecutionData, String clusterName,
      Set<String> containerSvcNameSet, InfrastructureMapping infrastructureMapping, Artifact artifact) {
    Validator.notNullCheck("containerSvcNameSet", containerSvcNameSet);
    if (containerSvcNameSet.isEmpty()) {
      String msg = "No container service names processed by the event";
      logger.error(msg);
      throw new WingsException(msg);
    }

    ContainerDeploymentInfo containerDeploymentInfo =
        ContainerDeploymentInfo.builder().containerSvcNameSet(containerSvcNameSet).clusterName(clusterName).build();

    // builder pattern doesn't quite work well here since we will have to duplicate the same setter code in multiple
    // places
    return instanceHelper.setValuesToDeploymentEvent(stateExecutionInstanceId, workflowExecution, phaseExecutionData,
        infrastructureMapping, artifact, containerDeploymentInfo);
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

  // TODO check if there is some missing functionality
  public void updateInstancesFromContainerInfo(
      Map<String, software.wings.beans.infrastructure.instance.ContainerDeploymentInfo>
          containerSvcNameDeploymentInfoMap,
      List<ContainerInfo> containerInfoList, String containerSvcNameNoRevision, InstanceType instanceType,
      String appId) {
    List<Instance> instanceList = Lists.newArrayList();
    // initialize the containerSvcNamesWithZeroInstances with the whole map first and then remove the ones which have
    // instances in it.
    Set<String> containerSvcNamesWithZeroInstances = Sets.newHashSet(containerSvcNameDeploymentInfoMap.keySet());
    for (ContainerInfo containerInfo : containerInfoList) {
      String containerSvcName = getContainerSvcName(containerInfo);
      software.wings.beans.infrastructure.instance.ContainerDeploymentInfo containerDeploymentInfo =
          containerSvcNameDeploymentInfoMap.get(containerSvcName);
      Validator.notNullCheck(
          "ContainerDeploymentDeploymentInfo is null for name: " + containerSvcName, containerDeploymentInfo);

      // remove the ones which have instances in it.
      containerSvcNamesWithZeroInstances.remove(containerSvcName);
      Instance instance = buildInstanceFromContainerInfo(containerDeploymentInfo.getAppId(),
          containerDeploymentInfo.getWorkflowExecutionId(), containerDeploymentInfo.getStateExecutionInstanceId(),
          containerDeploymentInfo.getInfraMappingId(), containerInfo);
      // instance could be returned null in some scenarios like artifact is null, etc
      if (instance != null) {
        instanceList.add(instance);
      }
    }

    // Save or update the container instances
    instanceService.saveOrUpdateContainerInstances(instanceType, containerSvcNameNoRevision, instanceList, appId);

    //  Cleans up container services that were either deleted or have a zero count.
    if (!containerSvcNamesWithZeroInstances.isEmpty()) {
      instanceService.deleteContainerDeploymentInfoAndInstances(
          containerSvcNamesWithZeroInstances, instanceType, appId);
    }
  }

  private Instance buildInstanceFromContainerInfo(String appId, String workflowExecutionId,
      String stateExecutionInstanceId, String infraMappingId, ContainerInfo containerInfo) {
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
    Validator.notNullCheck("InfrastructureMapping", infrastructureMapping);
    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionWithoutSummary(appId, workflowExecutionId);
    Validator.notNullCheck("WorkflowExecution", workflowExecution);
    StateExecutionInstance stateExecutionInstance =
        workflowExecutionService.getStateExecutionData(appId, stateExecutionInstanceId);
    Validator.notNullCheck("StateExecutionInstance", stateExecutionInstance);
    StateExecutionData stateExecutionData = stateExecutionInstance.getStateExecutionData();
    Validator.notNullCheck("StateExecutionData", stateExecutionData);

    if (stateExecutionData instanceof PhaseExecutionData) {
      PhaseExecutionData phaseExecutionData = (PhaseExecutionData) stateExecutionData;
      ExecutionArgs executionArgs = workflowExecution.getExecutionArgs();
      String serviceId = phaseExecutionData.getServiceId();
      List<Artifact> artifacts = executionArgs.getArtifacts();
      Artifact serviceArtifact = null;
      for (Artifact artifact : artifacts) {
        if (artifact.getServiceIds().contains(serviceId)) {
          serviceArtifact = artifact;
          break;
        }
      }

      if (serviceArtifact == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("artifact is null for stateExecutionInstance: " + stateExecutionInstanceId);
        }
      }

      final String infraMappingType = infrastructureMapping.getInfraMappingType();

      InstanceBuilder builder =
          instanceHelper.buildInstanceBase(workflowExecution, serviceArtifact, phaseExecutionData, infraMappingType);
      builder.containerInstanceKey(generateInstanceKeyForContainer(containerInfo));
      builder.instanceInfo(containerInfo);

      return builder.build();
    } else {
      String msg = "StateExecutionData doesn't refer to a Phase.";
      logger.error(msg);
      throw new WingsException(msg);
    }
  }

  public Instance buildInstanceFromContainerInfo(
      InfrastructureMapping infraMapping, ContainerInfo containerInfo, DeploymentInfo newDeploymentInfo) {
    InstanceBuilder builder = instanceHelper.buildInstanceBase(null, infraMapping, newDeploymentInfo);
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
}
