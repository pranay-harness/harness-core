package software.wings.service.impl.instance;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.HostElement;
import software.wings.api.InstanceChangeEvent;
import software.wings.api.InstanceChangeEvent.Builder;
import software.wings.api.PhaseExecutionData;
import software.wings.beans.Application;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.core.queue.Queue;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Validator;

import java.util.List;

/**
 * Both the normal instance and container instance are handled here.
 * Once it finds the deployment is of type container, it hands off the request to ContainerInstanceHelper.
 * @author rktummala on 09/11/17
 */
public class InstanceHelper {
  private static final Logger logger = LoggerFactory.getLogger(InstanceHelper.class);

  // This queue is used to asynchronously process all the instance information that the workflow touched upon.
  @Inject private Queue<InstanceChangeEvent> instanceChangeEventQueue;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private AppService appService;
  @Inject private InstanceUtil instanceUtil;
  @Inject private ContainerInstanceHelper containerInstanceHelper;
  @Inject private HostService hostService;

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
          infrastructureMappingService.get(appId, phaseExecutionData.getInfraMappingId());

      if (containerInstanceHelper.isContainerDeployment(infrastructureMapping)) {
        containerInstanceHelper.extractContainerInfoAndSendEvent(
            stateExecutionInstanceId, phaseExecutionData, workflowExecution, infrastructureMapping);
      } else {
        List<Instance> instanceList = Lists.newArrayList();

        for (ElementExecutionSummary summary : phaseExecutionData.getElementStatusSummary()) {
          List<InstanceStatusSummary> instanceStatusSummaries = summary.getInstanceStatusSummaries();
          if (isEmpty(instanceStatusSummaries)) {
            logger.debug("No instances to process");
            return;
          }
          for (InstanceStatusSummary instanceStatusSummary : instanceStatusSummaries) {
            if (shouldCaptureInstance(instanceStatusSummary.getStatus())) {
              Instance instance = buildInstanceUsingHostInfo(workflowExecution, artifact, instanceStatusSummary,
                  phaseExecutionData, infrastructureMapping.getInfraMappingType());
              if (instance != null) {
                instanceList.add(instance);
              }
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

  public Instance buildInstanceUsingHostInfo(WorkflowExecution workflowExecution, Artifact artifact,
      InstanceStatusSummary instanceStatusSummary, PhaseExecutionData phaseExecutionData, String infraMappingType) {
    HostElement host = instanceStatusSummary.getInstanceElement().getHost();
    Validator.notNullCheck("Host is null for workflow execution:" + workflowExecution.getWorkflowId(), host);

    Instance.Builder builder = buildInstanceBase(workflowExecution, artifact, phaseExecutionData, infraMappingType);
    String hostUuid = host.getUuid();
    if (hostUuid == null) {
      if (host.getEc2Instance() != null) {
        setInstanceInfoAndKey(builder, host.getEc2Instance(), infraMappingType, phaseExecutionData.getInfraMappingId());
      } else {
        logger.warn(
            "Cannot build host based instance info since both hostId and ec2Instance are null for workflow execution {}",
            workflowExecution.getUuid());
        return null;
      }
    } else {
      Host hostInfo = hostService.get(workflowExecution.getAppId(), workflowExecution.getEnvId(), hostUuid);
      Validator.notNullCheck("Host is null for workflow execution:" + workflowExecution.getWorkflowId(), hostInfo);
      setInstanceInfoAndKey(builder, hostInfo, infraMappingType, phaseExecutionData.getInfraMappingId());
    }
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
    InstanceInfo instanceInfo = null;
    HostInstanceKey hostInstanceKey =
        HostInstanceKey.builder().hostName(host.getHostName()).infraMappingId(infraMappingId).build();
    builder.withHostInstanceKey(hostInstanceKey);

    if (InfrastructureMappingType.AWS_SSH.getName().equals(infraMappingType)
        || InfrastructureMappingType.AWS_AWS_CODEDEPLOY.getName().equals(infraMappingType)
        || InfrastructureMappingType.AWS_AMI.getName().equals(infraMappingType)) {
      instanceInfo = Ec2InstanceInfo.Builder.anEc2InstanceInfo()
                         .withEc2Instance(host.getEc2Instance())
                         .withHostId(host.getUuid())
                         .withHostName(host.getHostName())
                         .withHostPublicDns(host.getPublicDns())
                         .build();
    } else if (InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.getName().equals(infraMappingType)) {
      instanceInfo = PhysicalHostInstanceInfo.Builder.aPhysicalHostInstanceInfo()
                         .withHostPublicDns(host.getPublicDns())
                         .withHostId(host.getUuid())
                         .withHostName(host.getHostName())
                         .build();
    }

    builder.withInstanceInfo(instanceInfo);
  }

  private void setInstanceInfoAndKey(Instance.Builder builder, com.amazonaws.services.ec2.model.Instance ec2Instance,
      String infraMappingType, String infraMappingId) {
    InstanceInfo instanceInfo = null;
    String privateDnsNameWithSuffix = ec2Instance.getPrivateDnsName();
    String privateDnsName =
        privateDnsNameWithSuffix.substring(0, privateDnsNameWithSuffix.lastIndexOf(".ec2.internal"));
    HostInstanceKey hostInstanceKey =
        HostInstanceKey.builder().hostName(privateDnsName).infraMappingId(infraMappingId).build();
    builder.withHostInstanceKey(hostInstanceKey);

    if (InfrastructureMappingType.AWS_AWS_CODEDEPLOY.getName().equals(infraMappingType)) {
      instanceInfo = Ec2InstanceInfo.Builder.anEc2InstanceInfo()
                         .withEc2Instance(ec2Instance)
                         .withHostName(privateDnsName)
                         .withHostPublicDns(ec2Instance.getPublicDnsName())
                         .build();
    }

    builder.withInstanceInfo(instanceInfo);
  }
}
