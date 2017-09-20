package software.wings.service.impl.instance;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CommandStepExecutionSummary;
import software.wings.api.ContainerDeploymentEvent;
import software.wings.api.ContainerServiceData;
import software.wings.api.PhaseExecutionData;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.common.Constants;
import software.wings.core.queue.Queue;
import software.wings.exception.WingsException;
import software.wings.service.impl.instance.sync.ContainerSync;
import software.wings.service.impl.instance.sync.request.ContainerFilter;
import software.wings.service.impl.instance.sync.request.ContainerSyncRequest;
import software.wings.service.impl.instance.sync.request.EcsFilter;
import software.wings.service.impl.instance.sync.request.KubernetesFilter;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StepExecutionSummary;
import software.wings.utils.Validator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
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
  @Inject private SettingsService settingsService;
  @Inject private GkeClusterService gkeClusterService;

  @Inject @Named("KubernetesInstanceSync") private ContainerSync kubernetesSyncService;
  @Inject @Named("EcsInstanceSync") private ContainerSync ecsSyncService;
  @Inject private InfrastructureMappingService infraMappingService;

  // This queue is used to asynchronously process all the container deployment information that the workflow touched
  // upon.
  @Inject private Queue<ContainerDeploymentEvent> containerDeploymentEventQueue;

  public boolean isContainerDeployment(InfrastructureMapping infrastructureMapping) {
    return (infrastructureMapping instanceof ContainerInfrastructureMapping
        || infrastructureMapping instanceof GcpKubernetesInfrastructureMapping
        || infrastructureMapping instanceof DirectKubernetesInfrastructureMapping);
  }

  public void extractContainerInfoAndSendEvent(String stateExecutionInstanceId, PhaseExecutionData phaseExecutionData,
      WorkflowExecution workflowExecution, InfrastructureMapping infrastructureMapping) {
    PhaseExecutionSummary phaseExecutionSummary = phaseExecutionData.getPhaseExecutionSummary();
    if (phaseExecutionSummary != null) {
      Map<String, PhaseStepExecutionSummary> phaseStepExecutionSummaryMap =
          phaseExecutionSummary.getPhaseStepExecutionSummaryMap();
      if (phaseStepExecutionSummaryMap != null) {
        for (String stepName : phaseStepExecutionSummaryMap.keySet()) {
          if (stepName.equals(Constants.DEPLOY_CONTAINERS) && phaseStepExecutionSummaryMap.containsKey(stepName)) {
            List<StepExecutionSummary> stepExecutionSummaryList =
                phaseStepExecutionSummaryMap.get(stepName).getStepExecutionSummaryList();
            // This was observed when the "deploy containers" step was executed in rollback and no commands were
            // executed since setup failed.
            if (stepExecutionSummaryList == null) {
              logger.debug(
                  "StepExecutionSummaryList is null for stateExecutionInstanceId: " + stateExecutionInstanceId);
              continue;
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
                                                 .collect(Collectors.toList()));
                }

                if (commandStepExecutionSummary.getNewInstanceData() != null) {
                  List<String> newcontainerSvcNames = commandStepExecutionSummary.getNewInstanceData()
                                                          .stream()
                                                          .map(ContainerServiceData::getName)
                                                          .collect(Collectors.toList());
                  containerSvcNameSet.addAll(newcontainerSvcNames);
                }

                if (containerSvcNameSet.isEmpty()) {
                  String msg = "Both old and new container services are empty. Cannot proceed for phase step "
                      + commandStepExecutionSummary.getServiceId();
                  logger.error(msg);
                  throw new WingsException(msg);
                }

                ContainerDeploymentEvent containerDeploymentEvent =
                    buildContainerDeploymentEvent(stateExecutionInstanceId, workflowExecution, phaseExecutionData,
                        clusterName, containerSvcNameSet, infrastructureMapping);
                containerDeploymentEventQueue.send(containerDeploymentEvent);
              }
            }
          }
        }
      }
    }
  }

  private ContainerDeploymentEvent buildContainerDeploymentEvent(String stateExecutionInstanceId,
      WorkflowExecution workflowExecution, PhaseExecutionData phaseExecutionData, String clusterName,
      Set<String> containerSvcNameSet, InfrastructureMapping infrastructureMapping) {
    Validator.notNullCheck("containerSvcNameSet", containerSvcNameSet);
    if (containerSvcNameSet.isEmpty()) {
      String msg = "No container service names processed by the event";
      logger.error(msg);
      throw new WingsException(msg);
    }

    PipelineSummary pipelineSummary = workflowExecution.getPipelineSummary();
    Application application = appService.get(workflowExecution.getAppId());
    Validator.notNullCheck("Application", application);
    EmbeddedUser triggeredBy = workflowExecution.getTriggeredBy();
    Validator.notNullCheck("triggeredBy", triggeredBy);
    String infraMappingType = infrastructureMapping.getInfraMappingType();

    String workflowName = instanceUtil.getWorkflowName(workflowExecution.getName());
    Validator.notNullCheck("WorkflowName", workflowName);

    InstanceType instanceType = instanceUtil.getInstanceType(infraMappingType);
    Validator.notNullCheck("InstanceType", instanceType);

    String[] containerSvcNames = containerSvcNameSet.toArray(new String[0]);
    String containerSvcNameNoRevision = getcontainerSvcNameNoRevision(instanceType, containerSvcNames[0]);

    ContainerDeploymentEvent.Builder containerDeploymentEvent =
        ContainerDeploymentEvent.Builder.aContainerDeploymentEvent()
            .withContainerSvcNameSet(containerSvcNameSet)
            .withAppId(workflowExecution.getAppId())
            .withAccountId(application.getAccountId())
            .withComputeProviderId(phaseExecutionData.getComputeProviderId())
            .withEnvId(workflowExecution.getEnvId())
            .withInfraMappingId(phaseExecutionData.getInfraMappingId())
            .withInstanceType(instanceType)
            .withPipelineExecutionId(pipelineSummary == null ? null : pipelineSummary.getPipelineId())
            .withServiceId(phaseExecutionData.getServiceId())
            .withStateExecutionInstanceId(stateExecutionInstanceId)
            .withWorkflowExecutionId(workflowExecution.getUuid())
            .withWorkflowId(workflowExecution.getWorkflowId())
            .withClusterName(clusterName)
            .withContainerSvcNameNoRevision(containerSvcNameNoRevision);

    return containerDeploymentEvent.build();
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

  private String getContainerSvcName(ContainerInfo containerInfo) {
    if (containerInfo instanceof KubernetesContainerInfo) {
      return ((KubernetesContainerInfo) containerInfo).getReplicationControllerName();
    } else if (containerInfo instanceof EcsContainerInfo) {
      return ((EcsContainerInfo) containerInfo).getTaskDefinitionArn();
    } else {
      throw new WingsException("Unsupported container info type:" + containerInfo.getClass().getCanonicalName());
    }
  }

  public void buildAndSaveInstancesFromContainerInfo(
      Map<String, ContainerDeploymentInfo> containerSvcNameDeploymentInfoMap, List<ContainerInfo> containerInfoList,
      String containerSvcNameWithoutRevision, InstanceType instanceType, String appId) {
    List<Instance> instanceList = Lists.newArrayList();
    // initialize the containerSvcNamesWithZeroInstances with the whole map first and then remove the ones which have
    // instances in it.
    Set<String> containerSvcNamesWithZeroInstances = Sets.newHashSet(containerSvcNameDeploymentInfoMap.keySet());
    for (ContainerInfo containerInfo : containerInfoList) {
      String containerSvcName = getContainerSvcName(containerInfo);
      ContainerDeploymentInfo containerDeploymentInfo = containerSvcNameDeploymentInfoMap.get(containerSvcName);
      // Added this to debug the null containerDeploymentInfo, which isn't expected.
      if (containerDeploymentInfo == null) {
        String msg = "ContainerDeploymentInfo is null for containerSvcName:" + containerSvcName;
        logger.error(msg);
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", msg);
      }

      // remove the ones which have instances in it.
      containerSvcNamesWithZeroInstances.remove(containerSvcName);
      Instance instance = buildInstanceFromContainerInfo(containerDeploymentInfo.getAppId(),
          containerDeploymentInfo.getWorkflowExecutionId(), containerDeploymentInfo.getStateExecutionInstanceId(),
          containerDeploymentInfo.getInfraMappingId(), containerInfo, instanceType);
      instanceList.add(instance);
    }

    // Save or update the container instances
    instanceService.saveOrUpdateContainerInstances(instanceType, containerSvcNameWithoutRevision, instanceList);

    //  Cleans up container services that were either deleted or have a zero count.
    if (!containerSvcNamesWithZeroInstances.isEmpty()) {
      instanceService.deleteContainerDeploymentInfoAndInstances(
          containerSvcNamesWithZeroInstances, instanceType, appId);
    }
  }

  private Instance buildInstanceFromContainerInfo(String appId, String workflowExecutionId,
      String stateExecutionInstanceId, String infraMappingId, ContainerInfo containerInfo, InstanceType instanceType) {
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
    Validator.notNullCheck("InfrastructureMapping", infrastructureMapping);
    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetailsWithoutGraph(appId, workflowExecutionId);
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
        logger.debug("artifact is null for stateExecutionInstance: " + stateExecutionInstanceId);
      }

      final String infraMappingType = infrastructureMapping.getInfraMappingType();

      Instance.Builder builder =
          instanceHelper.buildInstanceBase(workflowExecution, serviceArtifact, phaseExecutionData, infraMappingType);
      builder.withContainerInstanceKey(instanceUtil.generateInstanceKeyForContainer(containerInfo, instanceType));
      builder.withInstanceInfo(containerInfo);

      return builder.build();
    } else {
      String msg = "StateExecutionData doesn't refer to a Phase.";
      logger.error(msg);
      throw new WingsException(msg);
    }
  }

  public void buildAndSaveContainerDeploymentInfo(String containerSvcNameNoRevision,
      Collection<ContainerDeploymentInfo> containerDeploymentInfoSet, String appId, InstanceType instanceType,
      long syncTimestamp) {
    instanceService.saveOrUpdateContainerDeploymentInfo(
        containerSvcNameNoRevision, containerDeploymentInfoSet, appId, instanceType, syncTimestamp);
  }

  public ContainerDeploymentInfo buildContainerDeploymentInfo(
      String containerSvcName, ContainerDeploymentEvent containerDeploymentEvent, long syncTimestamp) {
    return ContainerDeploymentInfo.Builder.aContainerDeploymentInfo()
        .withAppId(containerDeploymentEvent.getAppId())
        .withAccountId(containerDeploymentEvent.getAccountId())
        .withContainerSvcName(containerSvcName)
        .withContainerSvcNameNoRevision(containerDeploymentEvent.getContainerSvcNameNoRevision())
        .withClusterName(containerDeploymentEvent.getClusterName())
        .withComputeProviderId(containerDeploymentEvent.getComputeProviderId())
        .withEnvId(containerDeploymentEvent.getEnvId())
        .withInfraMappingId(containerDeploymentEvent.getInfraMappingId())
        .withInstanceType(containerDeploymentEvent.getInstanceType())
        .withLastVisited(syncTimestamp)
        .withWorkflowId(containerDeploymentEvent.getWorkflowId())
        .withWorkflowExecutionId(containerDeploymentEvent.getWorkflowExecutionId())
        .withPipelineExecutionId(containerDeploymentEvent.getPipelineExecutionId())
        .withServiceId(containerDeploymentEvent.getServiceId())
        .withStateExecutionInstanceId(containerDeploymentEvent.getStateExecutionInstanceId())
        .build();
  }

  public ContainerSyncResponse getLatestInstancesFromCloud(Set<String> containerSvcNameSet, InstanceType instanceType,
      String appId, String infraMappingId, String clusterName, String computeProviderId) {
    ContainerFilter containerFilter =
        getContainerFilter(containerSvcNameSet, instanceType, appId, infraMappingId, clusterName, computeProviderId);
    Validator.notNullCheck("ContainerFilter", containerFilter);

    ContainerSyncRequest instanceSyncRequest =
        ContainerSyncRequest.Builder.aContainerSyncRequest().withFilter(containerFilter).build();
    if (instanceType == InstanceType.KUBERNETES_CONTAINER_INSTANCE) {
      return kubernetesSyncService.getInstances(instanceSyncRequest);
    } else if (instanceType == InstanceType.ECS_CONTAINER_INSTANCE) {
      return ecsSyncService.getInstances(instanceSyncRequest);
    } else {
      String msg = "Unsupported container instance type:" + instanceType;
      logger.error(msg);
      throw new WingsException(msg);
    }
  }

  private ContainerFilter getContainerFilter(Set<String> containerSvcNameSet, InstanceType instanceType, String appId,
      String infraMappingId, String clusterName, String computeProviderId) {
    ContainerFilter containerFilter;
    Validator.notNullCheck("InstanceType", instanceType);
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    Validator.notNullCheck("InfrastructureMapping", infrastructureMapping);
    if (instanceType == InstanceType.KUBERNETES_CONTAINER_INSTANCE) {
      containerFilter = KubernetesFilter.Builder.aKubernetesFilter()
                            .withClusterName(clusterName)
                            .withReplicationControllerNameSet(containerSvcNameSet)
                            .withKubernetesConfig(getKubernetesConfig(infrastructureMapping))
                            .build();

    } else if (instanceType == InstanceType.ECS_CONTAINER_INSTANCE) {
      if (!(infrastructureMapping instanceof EcsInfrastructureMapping)) {
        String msg = "Ecs doesn't support infrastructure mapping type :" + infrastructureMapping.getInfraMappingType();
        logger.error(msg);
        throw new WingsException(msg);
      }
      EcsInfrastructureMapping ecsInfrastructureMapping = (EcsInfrastructureMapping) infrastructureMapping;
      containerFilter = EcsFilter.Builder.anEcsFilter()
                            .withClusterName(clusterName)
                            .withServiceNameSet(containerSvcNameSet)
                            .withAwsComputeProviderId(computeProviderId)
                            .withRegion(ecsInfrastructureMapping.getRegion())
                            .build();

    } else {
      String msg = "Unsupported container instance type:" + instanceType;
      logger.error(msg);
      throw new WingsException(msg);
    }

    return containerFilter;
  }

  private KubernetesConfig getKubernetesConfig(InfrastructureMapping infrastructureMapping) {
    KubernetesConfig kubernetesConfig;

    String clusterName;
    if (infrastructureMapping instanceof GcpKubernetesInfrastructureMapping) {
      clusterName = ((GcpKubernetesInfrastructureMapping) infrastructureMapping).getClusterName();

      SettingAttribute computeProviderSetting =
          settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      kubernetesConfig = gkeClusterService.getCluster(computeProviderSetting, clusterName);
    } else {
      kubernetesConfig = ((DirectKubernetesInfrastructureMapping) infrastructureMapping).createKubernetesConfig();
    }

    return kubernetesConfig;
  }
}
