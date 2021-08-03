package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.dtos.DeploymentSummaryDTO;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.entities.ArtifactDetails;
import io.harness.exception.InvalidRequestException;
import io.harness.models.DeploymentEvent;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.service.deploymentsummary.DeploymentSummaryService;
import io.harness.service.infrastructuremapping.InfrastructureMappingService;
import io.harness.service.instancesync.InstanceSyncService;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;
import io.harness.service.instancesynchandlerfactory.InstanceSyncHandlerFactoryService;
import io.harness.yaml.core.StepSpecType;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.DX)
public class DeploymentEventListener implements OrchestrationEventHandler {
  private final OutcomeService outcomeService;
  private final InstanceSyncHandlerFactoryService instanceSyncHandlerFactoryService;
  private final InfrastructureMappingService infrastructureMappingService;
  private final InstanceSyncService instanceSyncService;
  private final InstanceInfoService instanceInfoService;
  private final DeploymentSummaryService deploymentSummaryService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    try {
      Ambiance ambiance = event.getAmbiance();
      ServiceOutcome serviceOutcome = getServiceOutcomeFromAmbiance(ambiance);
      InfrastructureOutcome infrastructureOutcome = getInfrastructureOutcomeFromAmbiance(ambiance);

      List<ServerInstanceInfo> serverInstanceInfoList = instanceInfoService.listServerInstances(
          ambiance, ((StepSpecType) event.getResolvedStepParameters()).getStepType());
      if (serverInstanceInfoList.isEmpty()) {
        return;
      }

      InfrastructureMappingDTO infrastructureMappingDTO =
          createInfrastructureMappingIfNotExists(ambiance, serviceOutcome, infrastructureOutcome);
      DeploymentSummaryDTO deploymentSummaryDTO = createDeploymentSummary(
          ambiance, serviceOutcome, infrastructureOutcome, infrastructureMappingDTO, serverInstanceInfoList);

      instanceSyncService.processInstanceSyncForNewDeployment(new DeploymentEvent(deploymentSummaryDTO, null));
    } catch (Exception exception) {
      log.error("Exception occured while handling event for instance sync", exception);
    }
  }

  // --------------------- PRIVATE METHODS ------------------------

  private InfrastructureMappingDTO createInfrastructureMappingIfNotExists(
      Ambiance ambiance, ServiceOutcome serviceOutcome, InfrastructureOutcome infrastructureOutcome) {
    AbstractInstanceSyncHandler abstractInstanceSyncHandler =
        instanceSyncHandlerFactoryService.getInstanceSyncHandler(infrastructureOutcome.getKind());

    InfrastructureMappingDTO infrastructureMappingDTO =
        InfrastructureMappingDTO.builder()
            .accountIdentifier(AmbianceUtils.getAccountId(ambiance))
            .orgIdentifier(AmbianceUtils.getOrgIdentifier(ambiance))
            .projectIdentifier(AmbianceUtils.getProjectIdentifier(ambiance))
            .serviceIdentifier(serviceOutcome.getIdentifier())
            .envIdentifier(infrastructureOutcome.getEnvironment().getIdentifier())
            .infrastructureKey(infrastructureOutcome.getInfrastructureKey())
            .infrastructureMappingType(abstractInstanceSyncHandler.getInfrastructureMappingType())
            .build();

    Optional<InfrastructureMappingDTO> infrastructureMappingDTOOptional =
        infrastructureMappingService.createNewOrReturnExistingInfrastructureMapping(infrastructureMappingDTO);
    if (infrastructureMappingDTOOptional.isPresent()) {
      return infrastructureMappingDTOOptional.get();
    } else {
      throw new InvalidRequestException("Failed to create infrastructure mapping for infrastructure key : "
          + infrastructureOutcome.getInfrastructureKey());
    }
  }

  private DeploymentSummaryDTO createDeploymentSummary(Ambiance ambiance, ServiceOutcome serviceOutcome,
      InfrastructureOutcome infrastructureOutcome, InfrastructureMappingDTO infrastructureMappingDTO,
      List<ServerInstanceInfo> serverInstanceInfoList) {
    AbstractInstanceSyncHandler abstractInstanceSyncHandler =
        instanceSyncHandlerFactoryService.getInstanceSyncHandler(infrastructureOutcome.getKind());

    DeploymentSummaryDTO deploymentSummaryDTO =
        DeploymentSummaryDTO.builder()
            .accountIdentifier(AmbianceUtils.getAccountId(ambiance))
            .orgIdentifier(AmbianceUtils.getOrgIdentifier(ambiance))
            .projectIdentifier(AmbianceUtils.getProjectIdentifier(ambiance))
            .pipelineExecutionId(ambiance.getPlanExecutionId())
            .pipelineExecutionName(ambiance.getMetadata().getPipelineIdentifier())
            .deployedByName(ambiance.getMetadata().getTriggerInfo().getTriggeredBy().getIdentifier())
            .deployedById(ambiance.getMetadata().getTriggerInfo().getTriggeredBy().getUuid())
            .artifactDetails(ArtifactDetails.builder()
                                 .artifactId(serviceOutcome.getArtifactsResult().getPrimary().getIdentifier())
                                 .tag(serviceOutcome.getArtifactsResult().getPrimary().getTag())
                                 .build())
            .infrastructureMappingId(infrastructureMappingDTO.getId())
            .infrastructureMapping(infrastructureMappingDTO)
            .deploymentInfoDTO(
                abstractInstanceSyncHandler.getDeploymentInfo(infrastructureOutcome, serverInstanceInfoList))
            .build();
    deploymentSummaryDTO = deploymentSummaryService.save(deploymentSummaryDTO);
    deploymentSummaryDTO.setServerInstanceInfoList(serverInstanceInfoList);

    return deploymentSummaryDTO;
  }

  private ServiceOutcome getServiceOutcomeFromAmbiance(Ambiance ambiance) {
    return (ServiceOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE));
  }

  private InfrastructureOutcome getInfrastructureOutcomeFromAmbiance(Ambiance ambiance) {
    return (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));
  }
}
