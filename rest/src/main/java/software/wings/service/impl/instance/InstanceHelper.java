package software.wings.service.impl.instance;

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
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.core.queue.Queue;
import software.wings.service.intfc.AppService;
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

  /**
   *   The phaseExecutionData is used to process the instance information that is used by the service and infra
   * dashboards. The instance processing happens asynchronously.
   *   @see InstanceChangeEventListener
   */
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
}
