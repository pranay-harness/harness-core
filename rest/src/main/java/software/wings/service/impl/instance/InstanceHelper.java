package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.sm.ExecutionStatus.FAILED;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.AmiStepExecutionSummary;
import software.wings.api.AwsAutoScalingGroupDeploymentInfo;
import software.wings.api.AwsCodeDeployDeploymentInfo;
import software.wings.api.CommandStepExecutionSummary;
import software.wings.api.ContainerServiceData;
import software.wings.api.DeploymentEvent;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentType;
import software.wings.api.HostElement;
import software.wings.api.PhaseExecutionData;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.core.queue.Queue;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Util;
import software.wings.utils.Validator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Both the normal instance and container instance are handled here.
 * Once it finds the deployment is of type container, it hands off the request to ContainerInstanceHelper.
 * @author rktummala on 09/11/17
 */
public class InstanceHelper {
  private static final Logger logger = LoggerFactory.getLogger(InstanceHelper.class);

  // This queue is used to asynchronously process all the instance information that the workflow touched upon.
  @Inject private Queue<DeploymentEvent> deploymentEventQueue;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private EnvironmentService environmentService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private AppService appService;
  @Inject private InstanceUtil instanceUtil;
  @Inject private ContainerInstanceHelper containerInstanceHelper;
  @Inject private HostService hostService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private InstanceService instanceService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private InstanceHandlerFactory instanceHandlerFactory;

  /**
   *   The phaseExecutionData is used to process the instance information that is used by the service and infra
   * dashboards. The instance processing happens asynchronously.
   *
   */
  public void extractInstanceOrContainerInfoBaseOnType(String stateExecutionInstanceId,
      StateExecutionData stateExecutionData, WorkflowStandardParams workflowStandardParams, String appId,
      WorkflowExecution workflowExecution) {
    try {
      if (!(stateExecutionData instanceof PhaseExecutionData)) {
        logger.error("stateExecutionData is not of type PhaseExecutionData");
        return;
      }

      PhaseExecutionData phaseExecutionData = (PhaseExecutionData) stateExecutionData;
      Validator.notNullCheck("PhaseExecutionData", phaseExecutionData);
      Validator.notNullCheck("ElementStatusSummary", phaseExecutionData.getElementStatusSummary());

      if (workflowStandardParams == null) {
        logger.warn("workflowStandardParams can't be null, skipping instance processing");
        return;
      }

      Artifact artifact = workflowStandardParams.getArtifactForService(phaseExecutionData.getServiceId());
      if (artifact == null) {
        logger.debug("artifact is null for stateExecutionInstance:" + stateExecutionInstanceId);
      }

      if (phaseExecutionData.getInfraMappingId() == null) {
        logger.debug(new StringBuilder()
                         .append("infraMappingId is null for appId:")
                         .append(appId)
                         .append(", WorkflowExecutionId:")
                         .append(workflowExecution.getUuid())
                         .toString());
        return;
      }
      InfrastructureMapping infrastructureMapping =
          infraMappingService.get(appId, phaseExecutionData.getInfraMappingId());

      // If its container based deployment
      if (containerInstanceHelper.isContainerDeployment(infrastructureMapping)) {
        Optional<DeploymentEvent> deploymentEvent = containerInstanceHelper.extractContainerInfoAndSendEvent(
            stateExecutionInstanceId, phaseExecutionData, workflowExecution, artifact, infrastructureMapping);

        if (deploymentEvent.isPresent()) {
          deploymentEventQueue.send(deploymentEvent.get());
        }
      } else {
        if (DeploymentType.AMI.name().equals(phaseExecutionData.getDeploymentType())) {
          List<String> autoScalingGroupNames = getASGFromAMIDeployment(phaseExecutionData, workflowExecution);
          AwsAutoScalingGroupDeploymentInfo deploymentInfo =
              AwsAutoScalingGroupDeploymentInfo.builder().autoScalingGroupNameList(autoScalingGroupNames).build();

          deploymentEventQueue.send(setValuesToDeploymentEvent(stateExecutionInstanceId, workflowExecution,
              phaseExecutionData, infrastructureMapping, artifact, deploymentInfo));
        } else if (DeploymentType.AWS_CODEDEPLOY.getDisplayName().equals(phaseExecutionData.getDeploymentType())) {
          String codeDeployDeploymentId = getCodeDeployDeploymentId(phaseExecutionData, workflowExecution);

          if (codeDeployDeploymentId == null) {
            logger.warn(new StringBuilder("Phase step execution summary null for Deploy for workflow:")
                            .append(workflowExecution.getName())
                            .append("Cant create deployment event")
                            .toString());
            return;
          }
          AwsCodeDeployDeploymentInfo deploymentInfo =
              AwsCodeDeployDeploymentInfo.builder().deploymentId(codeDeployDeploymentId).build();

          deploymentEventQueue.send(setValuesToDeploymentEvent(stateExecutionInstanceId, workflowExecution,
              phaseExecutionData, infrastructureMapping, artifact, deploymentInfo));
        } else if (InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.getName().equals(
                       infrastructureMapping.getInfraMappingType())
            || InfrastructureMappingType.AWS_SSH.getName().equals(infrastructureMapping.getInfraMappingType())) {
          List<Instance> instanceList = Lists.newArrayList();
          PhaseStepExecutionSummary phaseStepExecutionSummary =
              getDeployPhaseStep(phaseExecutionData, Constants.DEPLOY_SERVICE);
          if (phaseStepExecutionSummary != null) {
            boolean failed = checkIfAnyStepsFailed(phaseStepExecutionSummary);
            if (failed) {
              logger.info("Deploy Service Phase step failed, not capturing any instances");
              return;
            }
          }

          for (ElementExecutionSummary summary : phaseExecutionData.getElementStatusSummary()) {
            List<InstanceStatusSummary> instanceStatusSummaries = summary.getInstanceStatusSummaries();
            if (isEmpty(instanceStatusSummaries)) {
              logger.info("No instances to process");
              return;
            }

            for (InstanceStatusSummary instanceStatusSummary : instanceStatusSummaries) {
              if (shouldCaptureInstance(instanceStatusSummary.getStatus())) {
                Instance instance = buildInstanceUsingHostInfo(
                    workflowExecution, artifact, instanceStatusSummary, phaseExecutionData, infrastructureMapping);
                if (instance != null) {
                  instanceList.add(instance);
                }
              }
            }
          }
          instanceService.saveOrUpdate(instanceList);
        }
      }
    } catch (Exception ex) {
      // we deliberately don't throw back the exception since we don't want the workflow to be affected
      logger.error("Error while updating instance change information", ex);
    }
  }

  DeploymentEvent setValuesToDeploymentEvent(String stateExecutionInstanceId, WorkflowExecution workflowExecution,
      PhaseExecutionData phaseExecutionData, InfrastructureMapping infrastructureMapping, Artifact artifact,
      DeploymentInfo deploymentInfo) {
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

    deploymentInfo.setAppId(workflowExecution.getAppId());
    deploymentInfo.setAccountId(application.getAccountId());
    deploymentInfo.setInfraMappingId(phaseExecutionData.getInfraMappingId());
    deploymentInfo.setStateExecutionInstanceId(stateExecutionInstanceId);
    deploymentInfo.setWorkflowExecutionId(workflowExecution.getUuid());
    deploymentInfo.setWorkflowExecutionName(workflowExecution.getName());
    deploymentInfo.setWorkflowId(workflowExecution.getWorkflowId());

    if (artifact != null) {
      deploymentInfo.setArtifactId(artifact.getUuid());
      deploymentInfo.setArtifactName(artifact.getDisplayName());
      deploymentInfo.setArtifactStreamId(artifact.getArtifactStreamId());
      deploymentInfo.setArtifactSourceName(artifact.getArtifactSourceName());
      deploymentInfo.setArtifactBuildNum(artifact.getBuildNo());
    }

    if (pipelineSummary != null) {
      deploymentInfo.setPipelineExecutionId(pipelineSummary.getPipelineId());
      deploymentInfo.setPipelineExecutionName(pipelineSummary.getPipelineName());
    }

    deploymentInfo.setDeployedById(triggeredBy.getUuid());
    deploymentInfo.setDeployedByName(triggeredBy.getName());
    deploymentInfo.setDeployedAt(phaseExecutionData.getEndTs());

    return DeploymentEvent.builder().deploymentInfo(deploymentInfo).build();
  }

  private boolean checkIfAnyStepsFailed(PhaseStepExecutionSummary phaseStepExecutionSummary) {
    return phaseStepExecutionSummary.getStepExecutionSummaryList()
        .stream()
        .filter(stepExecutionSummary -> stepExecutionSummary.getStatus() == FAILED)
        .findFirst()
        .isPresent();
  }

  private PhaseStepExecutionSummary getDeployPhaseStep(PhaseExecutionData phaseExecutionData, String phaseStepName) {
    return phaseExecutionData.getPhaseExecutionSummary().getPhaseStepExecutionSummaryMap().get(phaseStepName);
  }

  /**
   * Returns the auto scaling group names
   */
  private List<String> getASGFromAMIDeployment(
      PhaseExecutionData phaseExecutionData, WorkflowExecution workflowExecution) throws HarnessException {
    List<String> autoScalingGroupNames = Lists.newArrayList();

    PhaseStepExecutionSummary phaseStepExecutionSummary =
        getDeployPhaseStep(phaseExecutionData, Constants.DEPLOY_SERVICE);
    if (phaseStepExecutionSummary != null) {
      Optional<StepExecutionSummary> stepExecutionSummaryOptional =
          phaseStepExecutionSummary.getStepExecutionSummaryList()
              .stream()
              .filter(stepExecutionSummary -> stepExecutionSummary instanceof AmiStepExecutionSummary)
              .findFirst();

      if (stepExecutionSummaryOptional.isPresent()) {
        StepExecutionSummary stepExecutionSummary = stepExecutionSummaryOptional.get();

        AmiStepExecutionSummary amiStepExecutionSummary = (AmiStepExecutionSummary) stepExecutionSummary;

        // Capture the instances of the new revision
        if (isNotEmpty(amiStepExecutionSummary.getNewInstanceData())) {
          List<String> asgList = amiStepExecutionSummary.getNewInstanceData()
                                     .stream()
                                     .map(ContainerServiceData::getName)
                                     .collect(Collectors.toList());
          if (isNotEmpty(asgList)) {
            autoScalingGroupNames.addAll(asgList);
          }
        }

        // Capture the instances of the old revision, note that the downsize operation need not bring the count
        // to zero.
        if (isNotEmpty(amiStepExecutionSummary.getOldInstanceData())) {
          List<String> asgList = amiStepExecutionSummary.getOldInstanceData()
                                     .stream()
                                     .map(ContainerServiceData::getName)
                                     .collect(Collectors.toList());
          if (isNotEmpty(asgList)) {
            autoScalingGroupNames.addAll(asgList);
          }
        }
      } else {
        throw new HarnessException(
            "Step execution summary null for AMI Deploy Step for workflow: " + workflowExecution.getName());
      }

    } else {
      throw new HarnessException(
          "Phase step execution summary null for AMI Deploy for workflow: " + workflowExecution.getName());
    }

    return autoScalingGroupNames;
  }

  private String getCodeDeployDeploymentId(PhaseExecutionData phaseExecutionData, WorkflowExecution workflowExecution)
      throws HarnessException {
    PhaseStepExecutionSummary phaseStepExecutionSummary =
        getDeployPhaseStep(phaseExecutionData, Constants.DEPLOY_SERVICE);
    if (phaseStepExecutionSummary != null) {
      Optional<StepExecutionSummary> stepExecutionSummaryOptional =
          phaseStepExecutionSummary.getStepExecutionSummaryList()
              .stream()
              .filter(stepExecutionSummary -> stepExecutionSummary instanceof CommandStepExecutionSummary)
              .findFirst();

      if (stepExecutionSummaryOptional.isPresent()) {
        StepExecutionSummary stepExecutionSummary = stepExecutionSummaryOptional.get();

        CommandStepExecutionSummary commandStepExecutionSummary = (CommandStepExecutionSummary) stepExecutionSummary;
        return commandStepExecutionSummary.getCodeDeployDeploymentId();

      } else {
        throw new HarnessException("Command step execution summary null for workflow: " + workflowExecution.getName());
      }

    } else {
      return null;
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
        return true;
      case FAILED:
      case ERROR:
      case ABORTED:
      default:
        return false;
    }
  }

  public Instance buildInstanceUsingEc2Instance(String instanceUuid,
      com.amazonaws.services.ec2.model.Instance ec2Instance, InfrastructureMapping infraMapping,
      DeploymentInfo deploymentInfo) {
    InstanceBuilder builder = buildInstanceBase(instanceUuid, infraMapping, deploymentInfo);
    setInstanceInfoAndKey(builder, ec2Instance, infraMapping.getUuid());
    return builder.build();
  }

  public Instance buildInstanceUsingHostInfo(WorkflowExecution workflowExecution, Artifact artifact,
      InstanceStatusSummary instanceStatusSummary, PhaseExecutionData phaseExecutionData,
      InfrastructureMapping infraMapping) {
    HostElement host = instanceStatusSummary.getInstanceElement().getHost();
    Validator.notNullCheck("Host is null for workflow execution:" + workflowExecution.getWorkflowId(), host);

    InstanceBuilder builder =
        buildInstanceBase(workflowExecution, artifact, phaseExecutionData, infraMapping.getInfraMappingType());
    String hostUuid = host.getUuid();

    String region = null;
    if (infraMapping instanceof AwsAmiInfrastructureMapping) {
      region = ((AwsAmiInfrastructureMapping) infraMapping).getRegion();
    } else if (infraMapping instanceof CodeDeployInfrastructureMapping) {
      region = ((CodeDeployInfrastructureMapping) infraMapping).getRegion();
    }

    if (hostUuid == null) {
      if (host.getEc2Instance() != null) {
        setInstanceInfoAndKey(builder, host.getEc2Instance(), phaseExecutionData.getInfraMappingId());
      } else if (host.getInstanceId() != null && region != null) {
        // TODO:: Avoid sequential fetch for Instance
        SettingAttribute cloudProviderSetting = settingsService.get(infraMapping.getComputeProviderSettingId());
        List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
            (Encryptable) cloudProviderSetting.getValue(), workflowExecution.getAppId(), workflowExecution.getUuid());
        AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();
        com.amazonaws.services.ec2.model.Instance instance =
            awsHelperService
                .describeEc2Instances(awsConfig, encryptionDetails, region,
                    new DescribeInstancesRequest().withInstanceIds(host.getInstanceId()))
                .getReservations()
                .stream()
                .findFirst()
                .orElse(new Reservation().withInstances(new ArrayList<>()))
                .getInstances()
                .stream()
                .findFirst()
                .orElse(null);
        if (instance != null) {
          setInstanceInfoAndKey(builder, instance, phaseExecutionData.getInfraMappingId());
        } else {
          logger.warn(
              "Cannot build host based instance info since instanceId is not found in AWS workflowId:{}, instanceId:{}",
              workflowExecution.getUuid(), host.getInstanceId());
          return null;
        }
      } else {
        logger.warn(
            "Cannot build host based instance info since both hostId and ec2Instance are null for workflow execution {}",
            workflowExecution.getUuid());
        return null;
      }
    } else {
      Host hostInfo = hostService.get(workflowExecution.getAppId(), workflowExecution.getEnvId(), hostUuid);
      Validator.notNullCheck("Host is null for workflow execution:" + workflowExecution.getWorkflowId(), hostInfo);
      setInstanceInfoAndKey(
          builder, hostInfo, infraMapping.getInfraMappingType(), phaseExecutionData.getInfraMappingId());
    }
    return builder.build();
  }

  public InstanceBuilder buildInstanceBase(WorkflowExecution workflowExecution, Artifact artifact,
      PhaseExecutionData phaseExecutionData, String infraMappingType) {
    PipelineSummary pipelineSummary = workflowExecution.getPipelineSummary();
    Application application = appService.get(workflowExecution.getAppId());
    Validator.notNullCheck("Application", application);
    EmbeddedUser triggeredBy = workflowExecution.getTriggeredBy();
    Validator.notNullCheck("triggeredBy", triggeredBy);

    InstanceBuilder builder = Instance.builder()
                                  .accountId(application.getAccountId())
                                  .appId(workflowExecution.getAppId())
                                  .appName(workflowExecution.getAppName())
                                  .envName(workflowExecution.getEnvName())
                                  .envId(workflowExecution.getEnvId())
                                  .envType(workflowExecution.getEnvType())
                                  .computeProviderId(phaseExecutionData.getComputeProviderId())
                                  .computeProviderName(phaseExecutionData.getComputeProviderName())
                                  .infraMappingId(phaseExecutionData.getInfraMappingId())
                                  .infraMappingType(infraMappingType)
                                  .lastDeployedAt(phaseExecutionData.getEndTs())
                                  .lastDeployedById(triggeredBy.getUuid())
                                  .lastDeployedByName(triggeredBy.getName())
                                  .serviceId(phaseExecutionData.getServiceId())
                                  .serviceName(phaseExecutionData.getServiceName())
                                  .lastWorkflowExecutionId(workflowExecution.getUuid());
    if (artifact != null) {
      builder.lastArtifactId(artifact.getUuid())
          .lastArtifactName(artifact.getDisplayName())
          .lastArtifactStreamId(artifact.getArtifactStreamId())
          .lastArtifactSourceName(artifact.getArtifactSourceName())
          .lastArtifactBuildNum(artifact.getBuildNo());
    }

    if (pipelineSummary != null) {
      builder.lastPipelineExecutionId(pipelineSummary.getPipelineId())
          .lastPipelineExecutionName(pipelineSummary.getPipelineName());
    }

    String workflowName = instanceUtil.getWorkflowName(workflowExecution.getName());
    Validator.notNullCheck("WorkflowName", workflowName);
    builder.lastWorkflowExecutionName(workflowName);

    instanceUtil.setInstanceType(builder, infraMappingType);

    return builder;
  }

  private void setInstanceInfoAndKey(
      InstanceBuilder builder, Host host, String infraMappingType, String infraMappingId) {
    InstanceInfo instanceInfo;
    HostInstanceKey hostInstanceKey =
        HostInstanceKey.builder().hostName(host.getHostName()).infraMappingId(infraMappingId).build();
    builder.hostInstanceKey(hostInstanceKey);

    if (InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.getName().equals(infraMappingType)) {
      instanceInfo = PhysicalHostInstanceInfo.builder()
                         .hostPublicDns(host.getPublicDns())
                         .hostId(host.getUuid())
                         .hostName(host.getHostName())
                         .build();
    } else {
      instanceInfo = Ec2InstanceInfo.builder()
                         .ec2Instance(host.getEc2Instance())
                         .hostId(host.getUuid())
                         .hostName(host.getHostName())
                         .hostPublicDns(host.getPublicDns())
                         .build();
    }

    builder.instanceInfo(instanceInfo);
  }

  public InstanceBuilder buildInstanceBase(
      String instanceId, InfrastructureMapping infraMapping, DeploymentInfo deploymentInfo) {
    InstanceBuilder builder = this.buildInstanceBase(instanceId, infraMapping);
    if (deploymentInfo != null) {
      builder.lastDeployedAt(deploymentInfo.getDeployedAt())
          .lastDeployedById(deploymentInfo.getDeployedById())
          .lastDeployedByName(deploymentInfo.getDeployedByName())
          .lastWorkflowExecutionId(deploymentInfo.getWorkflowExecutionId())
          .lastWorkflowExecutionName(deploymentInfo.getWorkflowExecutionName())
          .lastArtifactId(deploymentInfo.getArtifactId())
          .lastArtifactName(deploymentInfo.getArtifactName())
          .lastArtifactStreamId(deploymentInfo.getArtifactStreamId())
          .lastArtifactSourceName(deploymentInfo.getArtifactSourceName())
          .lastArtifactBuildNum(deploymentInfo.getArtifactBuildNum())
          .lastPipelineExecutionId(deploymentInfo.getPipelineExecutionId())
          .lastPipelineExecutionName(deploymentInfo.getPipelineExecutionName());
    }

    return builder;
  }

  public InstanceBuilder buildInstanceBase(String instanceUuid, InfrastructureMapping infraMapping) {
    String appId = infraMapping.getAppId();
    Application application = appService.get(appId);
    Validator.notNullCheck("Application is null for the given appId: " + appId, application);
    Environment environment = environmentService.get(appId, infraMapping.getEnvId(), false);
    Validator.notNullCheck("Environment is null for the given id: " + infraMapping.getEnvId(), environment);
    Service service = serviceResourceService.get(appId, infraMapping.getServiceId());
    Validator.notNullCheck("Service is null for the given id: " + infraMapping.getServiceId(), service);
    String infraMappingType = infraMapping.getInfraMappingType();

    if (instanceUuid == null) {
      instanceUuid = UUIDGenerator.getUuid();
    }

    InstanceBuilder builder = Instance.builder()
                                  .uuid(instanceUuid)
                                  .accountId(application.getAccountId())
                                  .appId(appId)
                                  .appName(application.getName())
                                  .envName(environment.getName())
                                  .envId(infraMapping.getEnvId())
                                  .envType(environment.getEnvironmentType())
                                  .computeProviderId(infraMapping.getComputeProviderSettingId())
                                  .computeProviderName(infraMapping.getComputeProviderName())
                                  .infraMappingId(infraMapping.getUuid())
                                  .infraMappingType(infraMappingType)
                                  .serviceId(infraMapping.getServiceId())
                                  .serviceName(service.getName());
    instanceUtil.setInstanceType(builder, infraMappingType);

    return builder;
  }

  private void setInstanceInfoAndKey(
      InstanceBuilder builder, com.amazonaws.services.ec2.model.Instance ec2Instance, String infraMappingId) {
    String privateDnsNameWithSuffix = ec2Instance.getPrivateDnsName();
    String privateDnsName = privateDnsNameWithSuffix.substring(0, privateDnsNameWithSuffix.indexOf('.'));
    HostInstanceKey hostInstanceKey =
        HostInstanceKey.builder().hostName(privateDnsName).infraMappingId(infraMappingId).build();
    builder.hostInstanceKey(hostInstanceKey);

    InstanceInfo instanceInfo = Ec2InstanceInfo.builder()
                                    .ec2Instance(ec2Instance)
                                    .hostName(privateDnsName)
                                    .hostPublicDns(ec2Instance.getPublicDnsName())
                                    .build();

    builder.instanceInfo(instanceInfo);
  }

  public void handleDeploymentEvent(DeploymentEvent deploymentEvent) {
    DeploymentInfo deploymentInfo = deploymentEvent.getDeploymentInfo();
    if (deploymentEvent == null) {
      throw new WingsException("Deployment info can not be null: " + deploymentEvent, WingsException.SERIOUS);
    }

    String infraMappingId = deploymentInfo.getInfraMappingId();
    String appId = deploymentInfo.getAppId();
    try (AcquiredLock lock =
             persistentLocker.acquireLock(InfrastructureMapping.class, infraMappingId, Duration.ofSeconds(120))) {
      InfrastructureMapping infraMapping = infraMappingService.get(appId, infraMappingId);
      Validator.notNullCheck("Infra mapping is null for the given id: " + infraMappingId, infraMapping);

      InfrastructureMappingType infrastructureMappingType =
          Util.getEnumFromString(InfrastructureMappingType.class, infraMapping.getInfraMappingType());
      if (isSupported(infrastructureMappingType)) {
        InstanceHandler instanceHandler = instanceHandlerFactory.getInstanceHandler(infrastructureMappingType);
        instanceHandler.handleNewDeployment(deploymentInfo);
      }
    } catch (Exception ex) {
      // We have to catch all kinds of runtime exceptions, log it and move on, otherwise the queue impl keeps retrying
      // forever in case of exception
      logger.error("Exception while processing phase completion event.", ex);
    }
  }

  private boolean isSupported(InfrastructureMappingType infrastructureMappingType) {
    if (InfrastructureMappingType.AWS_AWS_LAMBDA.equals(infrastructureMappingType)
        || InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.equals(infrastructureMappingType)) {
      return false;
    }
    return true;
  }
}