package software.wings.service.impl.instance;

import static software.wings.beans.infrastructure.instance.EcsContainerDeploymentInfo.Builder.anEcsContainerDeploymentInfo;
import static software.wings.beans.infrastructure.instance.KubernetesContainerDeploymentInfo.Builder.aKubernetesContainerDeploymentInfo;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

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
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.common.Constants;
import software.wings.core.queue.Queue;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StepExecutionSummary;
import software.wings.utils.Validator;

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

  // This queue is used to asynchronously process all the container deployment information that the workflow touched
  // upon.
  @Inject private Queue<ContainerDeploymentEvent> containerDeploymentEventQueue;

  public boolean isContainerDeployment(InfrastructureMapping infrastructureMapping) {
    return (infrastructureMapping instanceof ContainerInfrastructureMapping
        || infrastructureMapping instanceof GcpKubernetesInfrastructureMapping
        || infrastructureMapping instanceof DirectKubernetesInfrastructureMapping);
  }

  public void extractContainerInfo(PhaseExecutionData phaseExecutionData, WorkflowExecution workflowExecution,
      Artifact artifact, InfrastructureMapping infrastructureMapping) {
    PhaseExecutionSummary phaseExecutionSummary = phaseExecutionData.getPhaseExecutionSummary();
    if (phaseExecutionSummary != null) {
      Map<String, PhaseStepExecutionSummary> phaseStepExecutionSummaryMap =
          phaseExecutionSummary.getPhaseStepExecutionSummaryMap();
      if (phaseStepExecutionSummaryMap != null) {
        Set<String> stepNames = phaseStepExecutionSummaryMap.keySet();
        for (String stepName : stepNames) {
          if (stepName.equals(Constants.DEPLOY_CONTAINERS)) {
            PhaseStepExecutionSummary phaseStepExecutionSummary = phaseStepExecutionSummaryMap.get(stepName);
            if (phaseStepExecutionSummary == null) {
              continue;
            }
            List<StepExecutionSummary> stepExecutionSummaryList =
                phaseStepExecutionSummary.getStepExecutionSummaryList();
            for (StepExecutionSummary stepExecutionSummary : stepExecutionSummaryList) {
              if (stepExecutionSummary != null && stepExecutionSummary instanceof CommandStepExecutionSummary) {
                CommandStepExecutionSummary commandStepExecutionSummary =
                    (CommandStepExecutionSummary) stepExecutionSummary;
                String clusterName = commandStepExecutionSummary.getClusterName();
                List<String> containerServiceNameList = Lists.newArrayList();

                List<ContainerServiceData> oldPreviousInstanceCounts =
                    commandStepExecutionSummary.getOldPreviousInstanceCounts();
                if (oldPreviousInstanceCounts != null) {
                  List<String> oldContainerServiceNames = oldPreviousInstanceCounts.stream()
                                                              .map(ContainerServiceData::getName)
                                                              .collect(Collectors.toList());
                  containerServiceNameList.addAll(oldContainerServiceNames);
                }

                String newContainerServiceName = commandStepExecutionSummary.getNewContainerServiceName();
                if (newContainerServiceName != null) {
                  containerServiceNameList.add(newContainerServiceName);
                }

                if (containerServiceNameList.isEmpty()) {
                  String msg = "Both old and new container services are empty. Cannot proceed for phase step "
                      + commandStepExecutionSummary.getServiceId();
                  logger.error(msg);
                  throw new WingsException(msg);
                }

                ContainerDeploymentInfo containerDeploymentInfo = buildContainerDeploymentInfo(workflowExecution,
                    artifact, phaseExecutionData, clusterName, containerServiceNameList, infrastructureMapping);
                ContainerDeploymentEvent containerDeploymentEvent =
                    ContainerDeploymentEvent.Builder.aContainerDeploymentEvent()
                        .withContainerDeploymentInfo(containerDeploymentInfo)
                        .build();
                containerDeploymentEventQueue.send(containerDeploymentEvent);
              }
            }
          }
        }
      }
    }
  }

  private ContainerDeploymentInfo buildContainerDeploymentInfo(WorkflowExecution workflowExecution, Artifact artifact,
      PhaseExecutionData phaseExecutionData, String clusterName, List<String> containerServiceNameList,
      InfrastructureMapping infrastructureMapping) {
    PipelineSummary pipelineSummary = workflowExecution.getPipelineSummary();
    Application application = appService.get(workflowExecution.getAppId());
    Validator.notNullCheck("Application", application);
    EmbeddedUser triggeredBy = workflowExecution.getTriggeredBy();
    Validator.notNullCheck("triggeredBy", triggeredBy);
    String infraMappingType = infrastructureMapping.getInfraMappingType();

    String workflowName = instanceUtil.getWorkflowName(workflowExecution.getName());
    Validator.notNullCheck("WorkflowName", workflowName);

    if (InfrastructureMappingType.AWS_ECS.name().equals(infraMappingType)) {
      EcsInfrastructureMapping ecsInfrastructureMapping = (EcsInfrastructureMapping) infrastructureMapping;
      return anEcsContainerDeploymentInfo()
          .withEcsServiceNameList(containerServiceNameList)
          .withAwsRegion(ecsInfrastructureMapping.getRegion())
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
          .withLastPipelineId(pipelineSummary == null ? null : pipelineSummary.getPipelineId())
          .withLastPipelineName(pipelineSummary == null ? null : pipelineSummary.getPipelineName())
          .withLastDeployedAt(phaseExecutionData.getEndTs())
          .withLastDeployedById(triggeredBy.getUuid())
          .withLastDeployedByName(triggeredBy.getName())
          .withServiceId(phaseExecutionData.getServiceId())
          .withServiceName(phaseExecutionData.getServiceName())
          .withLastWorkflowId(workflowExecution.getUuid())
          .withLastWorkflowName(workflowName)
          .withClusterName(clusterName)
          .build();

    } else if (InfrastructureMappingType.GCP_KUBERNETES.name().equals(infraMappingType)
        || InfrastructureMappingType.DIRECT_KUBERNETES.name().equals(infraMappingType)) {
      return aKubernetesContainerDeploymentInfo()
          .withReplicationControllerNameList(containerServiceNameList)
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
          .withLastPipelineId(pipelineSummary == null ? null : pipelineSummary.getPipelineId())
          .withLastPipelineName(pipelineSummary == null ? null : pipelineSummary.getPipelineName())
          .withLastDeployedAt(phaseExecutionData.getEndTs())
          .withLastDeployedById(triggeredBy.getUuid())
          .withLastDeployedByName(triggeredBy.getName())
          .withServiceId(phaseExecutionData.getServiceId())
          .withServiceName(phaseExecutionData.getServiceName())
          .withLastWorkflowId(workflowExecution.getUuid())
          .withLastWorkflowName(workflowName)
          .withClusterName(clusterName)
          .build();
    }

    return null;
  }
}
