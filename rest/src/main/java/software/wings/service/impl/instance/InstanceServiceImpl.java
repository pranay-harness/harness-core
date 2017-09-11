package software.wings.service.impl.instance;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.HostElement;
import software.wings.api.InstanceChangeEvent;
import software.wings.api.InstanceChangeEvent.Builder;
import software.wings.api.PhaseExecutionData;
import software.wings.beans.Application;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.EcsContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.KubernetesContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.beans.infrastructure.instance.key.InstanceKey;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.core.queue.Queue;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.validation.Valid;

/**
 * @author rktummala on 8/13/17
 */
@Singleton
public class InstanceServiceImpl implements InstanceService {
  private static final Logger logger = LoggerFactory.getLogger(InstanceServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ArtifactService artifactService;
  @Inject private AppService appService;
  @Inject private ExecutorService executorService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  // This queue is used to asynchronously process all the instance information that the workflow touched upon.
  @Inject private Queue<InstanceChangeEvent> instanceChangeEventQueue;

  @Inject private ContainerInstanceHelper containerInstanceHelper;

  @Override
  public Instance save(Instance instance) {
    if (logger.isDebugEnabled()) {
      logger.debug("Begin - Instance save called for uuid:" + instance.getUuid()
          + " and infraMappingId:" + instance.getInfraMappingId());
    }
    if (!appService.exist(instance.getAppId())) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT);
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
    Validator.notNullCheck("Instance", instance);
    return wingsPersistence.delete(instance);
  }

  @Override
  public boolean delete(Set<String> instanceIdSet) {
    Query<Instance> query = wingsPersistence.createAuthorizedQuery(Instance.class);
    query.field("_id").in(instanceIdSet);
    return wingsPersistence.delete(query);
  }

  @Override
  public Instance buildInstance(WorkflowExecution workflowExecution, Artifact artifact,
      InstanceStatusSummary instanceStatusSummary, PhaseExecutionData phaseExecutionData, String infraMappingType) {
    HostElement host = instanceStatusSummary.getInstanceElement().getHost();
    Validator.notNullCheck("Host", host);
    PipelineSummary pipelineSummary = workflowExecution.getPipelineSummary();
    Application application = appService.get(workflowExecution.getAppId());
    Validator.notNullCheck("Application", application);
    EmbeddedUser triggeredBy = workflowExecution.getTriggeredBy();
    Validator.notNullCheck("triggeredBy", triggeredBy);

    Instance.Builder builder =
        Instance.Builder.anInstance()
            .withAccountId(application.getAccountId())
            .withAppId(workflowExecution.getAppId())
            .withAppName(workflowExecution.getAppName())
            .withLastArtifactId(artifact.getUuid())
            .withLastArtifactName(artifact.getDisplayName())
            .withLastArtifactStreamId(artifact.getArtifactStreamId())
            .withLastArtifactSourceName(artifact.getArtifactSourceName())
            .withLastArtifactBuildNum(artifact.getBuildNo())
            .withEnvName(workflowExecution.getEnvName())
            .withEnvId(workflowExecution.getEnvId())
            .withEnvType(workflowExecution.getEnvType())
            .withComputeProviderId(phaseExecutionData.getComputeProviderId())
            .withComputeProviderName(phaseExecutionData.getComputeProviderName())
            .withInfraMappingId(phaseExecutionData.getInfraMappingId())
            .withInfraMappingType(infraMappingType)
            .withLastPipelineExecutionId(pipelineSummary == null ? null : pipelineSummary.getPipelineId())
            .withLastPipelineExecutionName(pipelineSummary == null ? null : pipelineSummary.getPipelineName())
            .withLastDeployedAt(phaseExecutionData.getEndTs().longValue())
            .withLastDeployedById(triggeredBy.getUuid())
            .withLastDeployedByName(triggeredBy.getName())
            .withServiceId(phaseExecutionData.getServiceId())
            .withServiceName(phaseExecutionData.getServiceName())
            .withLastWorkflowExecutionId(workflowExecution.getUuid())
            .withLastWorkflowExecutionName(workflowExecution.getName());

    setInstanceType(builder, infraMappingType);
    setInstanceInfoAndKey(builder, host, infraMappingType, phaseExecutionData.getInfraMappingId());

    return builder.build();
  }

  private void setInstanceInfoAndKey(
      Instance.Builder builder, HostElement host, String infraMappingType, String infraMappingId) {
    InstanceInfo instanceInfo = null;
    HostInstanceKey hostInstanceKey = HostInstanceKey.Builder.aHostInstanceKey()
                                          .withHostName(host.getHostName())
                                          .withInfraMappingId(infraMappingId)
                                          .build();
    builder.withHostInstanceKey(hostInstanceKey);

    if (InfrastructureMappingType.AWS_SSH.getName().equals(infraMappingType)
        || InfrastructureMappingType.AWS_AWS_CODEDEPLOY.getName().equals(infraMappingType)) {
      if (host != null) {
        com.amazonaws.services.ec2.model.Instance ec2Instance = host.getEc2Instance();
        if (ec2Instance != null) {
          instanceInfo = Ec2InstanceInfo.Builder.anEc2InstanceInfo()
                             .withEc2Instance(ec2Instance)
                             .withHostId(host.getUuid())
                             .withHostName(host.getHostName())
                             .withHostPublicDns(host.getPublicDns())
                             .build();
        }
      }
    } else if (InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.getName().equals(infraMappingType)) {
      if (host != null) {
        instanceInfo = PhysicalHostInstanceInfo.Builder.aPhysicalHostInstanceInfo()
                           .withHostPublicDns(host.getPublicDns())
                           .withHostId(host.getUuid())
                           .withHostName(host.getHostName())
                           .build();
      }
    }

    if (instanceInfo != null) {
      builder.withInstanceInfo(instanceInfo);
    }
  }

  private ContainerInstanceKey generateInstanceKeyForContainer(
      ContainerInfo containerInfo, ContainerDeploymentInfo containerDeploymentInfo) {
    ContainerInstanceKey containerInstanceKey = null;

    if (containerDeploymentInfo instanceof KubernetesContainerDeploymentInfo) {
      KubernetesContainerInfo kubernetesContainerInfo = (KubernetesContainerInfo) containerInfo;
      containerInstanceKey = ContainerInstanceKey.Builder.aContainerInstanceKey()
                                 .withContainerId(kubernetesContainerInfo.getPodName())
                                 .build();

    } else if (containerDeploymentInfo instanceof EcsContainerDeploymentInfo) {
      EcsContainerInfo ecsContainerInfo = (EcsContainerInfo) containerInfo;
      containerInstanceKey =
          ContainerInstanceKey.Builder.aContainerInstanceKey().withContainerId(ecsContainerInfo.getTaskArn()).build();
    }

    return containerInstanceKey;
  }

  private void setInstanceType(Instance.Builder builder, String infraMappingType) {
    InstanceType instanceType = null;
    if (InfrastructureMappingType.DIRECT_KUBERNETES.name().equals(infraMappingType)
        || InfrastructureMappingType.GCP_KUBERNETES.name().equals(infraMappingType)) {
      instanceType = InstanceType.KUBERNETES_CONTAINER_INSTANCE;
    } else if (InfrastructureMappingType.AWS_ECS.name().equals(infraMappingType)) {
      instanceType = InstanceType.ECS_CONTAINER_INSTANCE;
    } else if (InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name().equals(infraMappingType)) {
      instanceType = InstanceType.PHYSICAL_HOST_INSTANCE;
    } else if (InfrastructureMappingType.AWS_SSH.name().equals(infraMappingType)
        || InfrastructureMappingType.AWS_AWS_CODEDEPLOY.name().equals(infraMappingType)) {
      instanceType = InstanceType.EC2_CLOUD_INSTANCE;
    }

    Validator.notNullCheck("InstanceType", instanceType);

    builder.withInstanceType(instanceType);
  }

  @Override
  public void buildAndSaveInstances(
      ContainerDeploymentInfo containerDeploymentInfo, List<ContainerInfo> containerInfoList) {
    Application application = appService.get(containerDeploymentInfo.getAppId());
    Validator.notNullCheck("Application", application);
    Validator.notNullCheck("ControllerInfoList", containerInfoList);

    Map<InstanceKey, Instance> currentInstanceMap = getCurrentInstancesInDB(containerDeploymentInfo, containerInfoList);

    List<Instance> newInstanceList = Lists.newArrayList();
    List<Instance> updateInstanceList = Lists.newArrayList();
    for (ContainerInfo containerInfo : containerInfoList) {
      ContainerInstanceKey instanceKey = generateInstanceKeyForContainer(containerInfo, containerDeploymentInfo);

      Instance existingInstance = currentInstanceMap.remove(instanceKey);
      if (existingInstance == null) {
        newInstanceList.add(
            buildInstanceFromContainerInfo(application, containerDeploymentInfo, containerInfo, instanceKey));
      } else {
        updateInstanceList.add(
            buildInstanceFromContainerInfo(application, containerDeploymentInfo, containerInfo, instanceKey));
      }
    }

    save(newInstanceList);
    update(updateInstanceList);
    Set<String> staleIds = currentInstanceMap.values().stream().map(Instance::getUuid).collect(toSet());
    delete(staleIds);
  }

  private Map<InstanceKey, Instance> getCurrentInstancesInDB(
      ContainerDeploymentInfo containerDeploymentInfo, List<ContainerInfo> containerInfoList) {
    Query<Instance> query = wingsPersistence.createAuthorizedQuery(Instance.class).disableValidation();
    Map<InstanceKey, Instance> instanceMap = Maps.newHashMap();
    if (containerDeploymentInfo instanceof KubernetesContainerDeploymentInfo) {
      KubernetesContainerDeploymentInfo kubernetesContainerDeploymentInfo =
          (KubernetesContainerDeploymentInfo) containerDeploymentInfo;
      query.field("instanceInfo.replicationControllerName")
          .in(kubernetesContainerDeploymentInfo.getReplicationControllerNameList());

    } else if (containerDeploymentInfo instanceof EcsContainerDeploymentInfo) {
      EcsContainerDeploymentInfo ecsContainerDeploymentInfo = (EcsContainerDeploymentInfo) containerDeploymentInfo;
      query.field("instanceInfo.serviceName").in(ecsContainerDeploymentInfo.getEcsServiceNameList());

    } else {
      throw new WingsException(
          "Unsupported ContainerDeployementInfo type:" + containerDeploymentInfo.getClass().getCanonicalName());
    }

    List<Instance> instanceList = query.asList();
    instanceMap = instanceList.stream().collect(toMap(Instance::getContainerInstanceKey, instance -> instance));

    return instanceMap;
  }

  private Instance buildInstanceFromContainerInfo(Application application,
      ContainerDeploymentInfo containerDeploymentInfo, ContainerInfo containerInfo, ContainerInstanceKey instanceKey) {
    Instance.Builder instanceBuilder =
        Instance.Builder.anInstance()
            .withAccountId(application.getAccountId())
            .withAppId(application.getAppId())
            .withAppName(application.getName())
            .withLastArtifactId(containerDeploymentInfo.getLastArtifactId())
            .withLastArtifactName(containerDeploymentInfo.getLastArtifactName())
            .withLastArtifactStreamId(containerDeploymentInfo.getLastArtifactStreamId())
            .withLastArtifactSourceName(containerDeploymentInfo.getLastArtifactSourceName())
            .withLastArtifactBuildNum(containerDeploymentInfo.getLastArtifactBuildNum())
            .withEnvName(containerDeploymentInfo.getEnvName())
            .withEnvId(containerDeploymentInfo.getEnvId())
            .withEnvType(containerDeploymentInfo.getEnvType())
            .withComputeProviderId(containerDeploymentInfo.getComputeProviderId())
            .withComputeProviderName(containerDeploymentInfo.getComputeProviderName())
            .withInfraMappingId(containerDeploymentInfo.getInfraMappingId())
            .withInfraMappingType(containerDeploymentInfo.getInfraMappingType())
            .withLastPipelineExecutionId(containerDeploymentInfo.getLastPipelineId())
            .withLastPipelineExecutionName(containerDeploymentInfo.getLastPipelineName())
            .withLastDeployedAt(containerDeploymentInfo.getLastDeployedAt())
            .withLastDeployedById(containerDeploymentInfo.getLastDeployedById())
            .withLastDeployedByName(containerDeploymentInfo.getLastDeployedByName())
            .withServiceId(containerDeploymentInfo.getServiceId())
            .withServiceName(containerDeploymentInfo.getServiceName())
            .withLastWorkflowExecutionId(containerDeploymentInfo.getLastWorkflowId())
            .withLastWorkflowExecutionName(containerDeploymentInfo.getLastWorkflowName());

    setInstanceType(instanceBuilder, containerDeploymentInfo.getInfraMappingType());
    instanceBuilder.withInstanceInfo(containerInfo);
    instanceBuilder.withContainerInstanceKey(instanceKey);
    return instanceBuilder.build();
  }

  /**
   *   The phaseExecutionData is used to process the instance information that is used by the service and infra
   * dashboards. The instance processing happens asynchronously.
   *   @see InstanceChangeEventListener
   */
  @Override
  public void extractInstanceOrContainerInfoBaseOnType(StateExecutionData stateExecutionData,
      WorkflowStandardParams workflowStandardParams, String appId, WorkflowExecution workflowExecution) {
    try {
      if (!(stateExecutionData instanceof PhaseExecutionData)) {
        logger.error("stateExecutionData is not of type PhaseExecutionData");
        return;
      }

      PhaseExecutionData phaseExecutionData = (PhaseExecutionData) stateExecutionData;
      Validator.notNullCheck("PhaseExecutionData", phaseExecutionData);
      Validator.notNullCheck("ElementStatusSummary", phaseExecutionData.getElementStatusSummary());

      if (workflowStandardParams == null) {
        logger.error("workflowStandardParams can't be null");
        return;
      }

      Artifact artifact = workflowStandardParams.getArtifactForService(phaseExecutionData.getServiceId());
      if (artifact == null) {
        logger.error("artifact can't be null");
        return;
      }

      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(appId, phaseExecutionData.getInfraMappingId());

      if (containerInstanceHelper.isContainerDeployment(infrastructureMapping)) {
        containerInstanceHelper.extractContainerInfo(
            phaseExecutionData, workflowExecution, artifact, infrastructureMapping);
      } else {
        List<Instance> instanceList = Lists.newArrayList();

        for (ElementExecutionSummary summary : phaseExecutionData.getElementStatusSummary()) {
          List<InstanceStatusSummary> instanceStatusSummaries = summary.getInstanceStatusSummaries();
          if (instanceStatusSummaries == null) {
            logger.debug("No instances to process");
            return;
          }
          for (InstanceStatusSummary instanceStatusSummary : instanceStatusSummaries) {
            if (shouldCaptureInstance(instanceStatusSummary.getStatus())) {
              Instance instance = buildInstance(workflowExecution, artifact, instanceStatusSummary, phaseExecutionData,
                  infrastructureMapping.getInfraMappingType());
              instanceList.add(instance);
            }
          }
        }

        InstanceChangeEvent instanceChangeEvent =
            Builder.anInstanceChangeEvent().withInstanceList(instanceList).build();
        instanceChangeEventQueue.send(instanceChangeEvent);
      }

    } catch (Exception ex) {
      // we deliberately don't throw back the exception since we don't want the workflow to be affected
      logger.error("Error while updating instance change information", ex);
    }
  }

  /**
   * At the end of the phase, the instance can only be in one of the following states.
   */
  private boolean shouldCaptureInstance(ExecutionStatus instanceExecutionStatus) {
    // Instance would have a status but just in case.
    if (instanceExecutionStatus == null) {
      return false;
    }

    switch (instanceExecutionStatus) {
      case SUCCESS:
      case FAILED:
      case ERROR:
      case ABORTED:
        return true;
      default:
        return false;
    }
  }
}
