package software.wings.service.impl.instance;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static software.wings.common.Constants.DEPLOY_SERVICE;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.AmiStepExecutionSummary;
import software.wings.api.DeploymentType;
import software.wings.api.HostElement;
import software.wings.api.InstanceChangeEvent;
import software.wings.api.PhaseExecutionData;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.core.queue.Queue;
import software.wings.exception.HarnessException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Both the normal instance and container instance are handled here.
 * Once it finds the deployment is of type container, it hands off the request to ContainerInstanceHelper.
 * @author rktummala on 09/11/17
 */
public class InstanceHelper {
  private static final Logger logger = LoggerFactory.getLogger(InstanceHelper.class);

  // This queue is used to asynchronously process all the instance information that the workflow touched upon.
  @Inject private Queue<InstanceChangeEvent> instanceChangeEventQueue;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private AppService appService;
  @Inject private InstanceUtil instanceUtil;
  @Inject private ContainerInstanceHelper containerInstanceHelper;
  @Inject private HostService hostService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;

  /**
   *   The phaseExecutionData is used to process the instance information that is used by the service and infra
   * dashboards. The instance processing happens asynchronously.
   *   @see InstanceChangeEventListener
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

      InfrastructureMapping infrastructureMapping =
          infraMappingService.get(appId, phaseExecutionData.getInfraMappingId());

      if (containerInstanceHelper.isContainerDeployment(infrastructureMapping)) {
        containerInstanceHelper.extractContainerInfoAndSendEvent(
            stateExecutionInstanceId, phaseExecutionData, workflowExecution, infrastructureMapping);
      } else {
        List<String> autoScalingGroupNames = null;
        List<Instance> totalInstanceList = Lists.newArrayList();

        if (DeploymentType.AMI.name().equals(phaseExecutionData.getDeploymentType())) {
          autoScalingGroupNames = getASGFromAMIDeploymentAndLoadInstances(
              infrastructureMapping, phaseExecutionData, workflowExecution, artifact, totalInstanceList);
        } else {
          for (ElementExecutionSummary summary : phaseExecutionData.getElementStatusSummary()) {
            List<InstanceStatusSummary> instanceStatusSummaries = summary.getInstanceStatusSummaries();
            if (isEmpty(instanceStatusSummaries)) {
              logger.debug("No instances to process");
              return;
            }
            for (InstanceStatusSummary instanceStatusSummary : instanceStatusSummaries) {
              if (shouldCaptureInstance(instanceStatusSummary.getStatus())) {
                Instance instance = buildInstanceUsingHostInfo(
                    workflowExecution, artifact, instanceStatusSummary, phaseExecutionData, infrastructureMapping);
                if (instance != null) {
                  totalInstanceList.add(instance);
                }
              }
            }
          }
        }

        InstanceChangeEvent instanceChangeEvent = InstanceChangeEvent.builder()
                                                      .instanceList(totalInstanceList)
                                                      .autoScalingGroupList(autoScalingGroupNames)
                                                      .appId(appId)
                                                      .build();
        instanceChangeEventQueue.send(instanceChangeEvent);
      }

    } catch (Exception ex) {
      // we deliberately don't throw back the exception since we don't want the workflow to be affected
      logger.error("Error while updating instance change information", ex);
    }
  }

  /**
   * Returns the auto scaling group names and also loads the instances to the totalInstanceList
   */
  private List<String> getASGFromAMIDeploymentAndLoadInstances(InfrastructureMapping infrastructureMapping,
      PhaseExecutionData phaseExecutionData, WorkflowExecution workflowExecution, Artifact artifact,
      List<Instance> totalInstanceList) throws HarnessException {
    List<String> autoScalingGroupNames = Lists.newArrayList();
    AwsAmiInfrastructureMapping awsAmiInfrastructureMapping = (AwsAmiInfrastructureMapping) infrastructureMapping;

    Optional<PhaseStepExecutionSummary> phaseStepSummaryOptional =
        phaseExecutionData.getPhaseExecutionSummary()
            .getPhaseStepExecutionSummaryMap()
            .entrySet()
            .stream()
            .filter(mapEntry -> DEPLOY_SERVICE.equals(mapEntry.getKey()))
            .map(mapEntry -> mapEntry.getValue())
            .findFirst();
    if (phaseStepSummaryOptional.isPresent()) {
      PhaseStepExecutionSummary phaseStepExecutionSummary = phaseStepSummaryOptional.get();
      Optional<StepExecutionSummary> stepExecutionSummaryOptional =
          phaseStepExecutionSummary.getStepExecutionSummaryList()
              .stream()
              .filter(stepExecutionSummary -> stepExecutionSummary instanceof AmiStepExecutionSummary)
              .findFirst();

      if (stepExecutionSummaryOptional.isPresent()) {
        StepExecutionSummary stepExecutionSummary = stepExecutionSummaryOptional.get();

        AmiStepExecutionSummary amiStepExecutionSummary = (AmiStepExecutionSummary) stepExecutionSummary;

        // Capture the instances of the new revision
        if (!isEmpty(amiStepExecutionSummary.getNewInstanceData())) {
          amiStepExecutionSummary.getNewInstanceData().stream().forEach(containerServiceData -> {
            List<Instance> instanceList = getInstancesFromAutoScalingGroup(awsAmiInfrastructureMapping.getRegion(),
                containerServiceData.getName(), phaseExecutionData, workflowExecution, artifact,
                infrastructureMapping.getInfraMappingType());
            totalInstanceList.addAll(instanceList);
            autoScalingGroupNames.add(containerServiceData.getName());
          });
        }

        // Capture the instances of the old revision, note that the downsize operation need not bring the count
        // to zero.
        if (!isEmpty(amiStepExecutionSummary.getOldInstanceData())) {
          amiStepExecutionSummary.getOldInstanceData().stream().forEach(containerServiceData -> {
            List<Instance> instanceList = getInstancesFromAutoScalingGroup(awsAmiInfrastructureMapping.getRegion(),
                containerServiceData.getName(), phaseExecutionData, workflowExecution, artifact,
                infrastructureMapping.getInfraMappingType());
            totalInstanceList.addAll(instanceList);
            autoScalingGroupNames.add(containerServiceData.getName());
          });
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

  private List<Instance> getInstancesFromAutoScalingGroup(String region, String autoScalingGroupName,
      PhaseExecutionData phaseExecutionData, WorkflowExecution workflowExecution, Artifact artifact,
      String infraMappingType) {
    String computeProviderId = phaseExecutionData.getComputeProviderId();
    SettingAttribute settingAttribute = settingsService.get(computeProviderId);
    Validator.notNullCheck("Aws config not found with the given id:" + computeProviderId, settingAttribute);
    InfrastructureMapping infrastructureMapping =
        infraMappingService.get(workflowExecution.getAppId(), phaseExecutionData.getInfraMappingId());
    Validator.notNullCheck(
        "Infrastructure mapping not found with the given id:" + phaseExecutionData.getInfraMappingId(),
        infrastructureMapping);

    List<Instance> instanceList = Lists.newArrayList();
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(awsConfig, null, null);

    DescribeInstancesResult describeInstancesResult =
        awsHelperService.describeAutoScalingGroupInstances(awsConfig, encryptionDetails, region, autoScalingGroupName);
    describeInstancesResult.getReservations().stream().forEach(
        reservation -> reservation.getInstances().stream().forEach(ec2Instance -> {
          Instance instance = buildInstanceUsingEc2InstanceInfo(
              workflowExecution, artifact, ec2Instance, phaseExecutionData, infraMappingType, autoScalingGroupName);
          instanceList.add(instance);
        }));
    return instanceList;
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

  public Instance buildInstanceUsingHostInfo(WorkflowExecution workflowExecution, Artifact artifact,
      InstanceStatusSummary instanceStatusSummary, PhaseExecutionData phaseExecutionData,
      InfrastructureMapping infraMapping) {
    HostElement host = instanceStatusSummary.getInstanceElement().getHost();
    Validator.notNullCheck("Host is null for workflow execution:" + workflowExecution.getWorkflowId(), host);

    Instance.Builder builder =
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
        setInstanceInfoAndKey(builder, host.getEc2Instance(), phaseExecutionData.getInfraMappingId(), null);
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
          setInstanceInfoAndKey(builder, instance, phaseExecutionData.getInfraMappingId(), null);
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

  public Instance buildInstanceUsingEc2InstanceInfo(WorkflowExecution workflowExecution, Artifact artifact,
      com.amazonaws.services.ec2.model.Instance ec2Instance, PhaseExecutionData phaseExecutionData,
      String infraMappingType, String autoScalingGroupName) {
    Instance.Builder builder = buildInstanceBase(workflowExecution, artifact, phaseExecutionData, infraMappingType);
    setInstanceInfoAndKey(builder, ec2Instance, phaseExecutionData.getInfraMappingId(), autoScalingGroupName);
    return builder.build();
  }

  public Instance.Builder buildInstanceBase(WorkflowExecution workflowExecution, Artifact artifact,
      PhaseExecutionData phaseExecutionData, String infraMappingType) {
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
            .withLastArtifactId(artifact == null ? null : artifact.getUuid())
            .withLastArtifactName(artifact == null ? null : artifact.getDisplayName())
            .withLastArtifactStreamId(artifact == null ? null : artifact.getArtifactStreamId())
            .withLastArtifactSourceName(artifact == null ? null : artifact.getArtifactSourceName())
            .withLastArtifactBuildNum(artifact == null ? null : artifact.getBuildNo())
            .withEnvName(workflowExecution.getEnvName())
            .withEnvId(workflowExecution.getEnvId())
            .withEnvType(workflowExecution.getEnvType())
            .withComputeProviderId(phaseExecutionData.getComputeProviderId())
            .withComputeProviderName(phaseExecutionData.getComputeProviderName())
            .withInfraMappingId(phaseExecutionData.getInfraMappingId())
            .withInfraMappingType(infraMappingType)
            .withLastPipelineExecutionId(pipelineSummary == null ? null : pipelineSummary.getPipelineId())
            .withLastPipelineExecutionName(pipelineSummary == null ? null : pipelineSummary.getPipelineName())
            .withLastDeployedAt(phaseExecutionData.getEndTs())
            .withLastDeployedById(triggeredBy.getUuid())
            .withLastDeployedByName(triggeredBy.getName())
            .withServiceId(phaseExecutionData.getServiceId())
            .withServiceName(phaseExecutionData.getServiceName())
            .withLastWorkflowExecutionId(workflowExecution.getUuid());
    String workflowName = instanceUtil.getWorkflowName(workflowExecution.getName());
    Validator.notNullCheck("WorkflowName", workflowName);
    builder.withLastWorkflowExecutionName(workflowName);

    instanceUtil.setInstanceType(builder, infraMappingType);

    return builder;
  }

  private void setInstanceInfoAndKey(
      Instance.Builder builder, Host host, String infraMappingType, String infraMappingId) {
    InstanceInfo instanceInfo;
    HostInstanceKey hostInstanceKey =
        HostInstanceKey.builder().hostName(host.getHostName()).infraMappingId(infraMappingId).build();
    builder.withHostInstanceKey(hostInstanceKey);

    if (InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.getName().equals(infraMappingType)) {
      instanceInfo = PhysicalHostInstanceInfo.Builder.aPhysicalHostInstanceInfo()
                         .withHostPublicDns(host.getPublicDns())
                         .withHostId(host.getUuid())
                         .withHostName(host.getHostName())
                         .build();
    } else {
      instanceInfo = Ec2InstanceInfo.Builder.anEc2InstanceInfo()
                         .withEc2Instance(host.getEc2Instance())
                         .withHostId(host.getUuid())
                         .withHostName(host.getHostName())
                         .withHostPublicDns(host.getPublicDns())
                         .build();
    }

    builder.withInstanceInfo(instanceInfo);
  }

  private void setInstanceInfoAndKey(Instance.Builder builder, com.amazonaws.services.ec2.model.Instance ec2Instance,
      String infraMappingId, String autoScalingGroupName) {
    String privateDnsNameWithSuffix = ec2Instance.getPrivateDnsName();
    String privateDnsName = privateDnsNameWithSuffix.substring(0, privateDnsNameWithSuffix.indexOf('.'));
    HostInstanceKey hostInstanceKey =
        HostInstanceKey.builder().hostName(privateDnsName).infraMappingId(infraMappingId).build();
    builder.withHostInstanceKey(hostInstanceKey);

    InstanceInfo instanceInfo = Ec2InstanceInfo.Builder.anEc2InstanceInfo()
                                    .withEc2Instance(ec2Instance)
                                    .withHostName(privateDnsName)
                                    .withAutoScalingGroupName(autoScalingGroupName)
                                    .withHostPublicDns(ec2Instance.getPublicDnsName())
                                    .build();

    builder.withInstanceInfo(instanceInfo);
  }
}
