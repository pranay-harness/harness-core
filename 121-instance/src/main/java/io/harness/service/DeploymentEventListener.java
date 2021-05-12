package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.entities.DeploymentSummary;
import io.harness.entities.infrastructureMapping.InfrastructureMapping;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pojo.DeploymentEvent;
import io.harness.repositories.infrastructuremapping.InfrastructureMappingRepository;
import io.harness.service.instancehandlerfactory.InstanceHandlerFactoryService;
import io.harness.service.instancesync.InstanceSyncService;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.DX)
public class DeploymentEventListener implements AsyncOrchestrationEventHandler {
  private OutcomeService outcomeService;
  private InstanceHandlerFactoryService instanceHandlerFactoryService;
  private InfrastructureMappingRepository infrastructureMappingRepository;
  private InstanceSyncService instanceSyncService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    try {
      // TODO check if event is of deployment step or not, if yes then consume otherwise ignore

      InfrastructureMapping infrastructureMapping = createInfrastructureMappingIfNew(event);
      DeploymentEvent deploymentEvent = prepareDeploymentEvent(event, infrastructureMapping);

      instanceSyncService.processDeploymentEvent(deploymentEvent);
    } catch (Exception exception) {
      log.error("Exception occured while handling event for instance sync", exception);
    }
  }

  // --------------------- PRIVATE METHODS ------------------------

  private InfrastructureMapping createInfrastructureMappingIfNew(OrchestrationEvent event) {
    // Fetch handler to extract infrastructure maping
    // pass on ambiance to handler to return corresponding infrastructure mapping object
    // check if the infrastructure mapping is already present in DB or not
    // TODO If already present check if we need to update it or not
    // If not present, then create new infrastructure mapping in DB and return

    Ambiance ambiance = event.getAmbiance();
    InstanceHandler instanceHandler = getInstanceHandler(ambiance);
    InfrastructureMapping infrastructureMapping = instanceHandler.getInfrastructureMapping(ambiance);

    // Check if infrastructure mapping already exists in DB or not, if not, then create a new record
    Optional<InfrastructureMapping> infrastructureMappingInDBOptional =
        infrastructureMappingRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndId(
            AmbianceHelper.getAccountId(ambiance), AmbianceHelper.getOrgIdentifier(ambiance),
            AmbianceHelper.getProjectIdentifier(ambiance), infrastructureMapping.getId());
    if (!infrastructureMappingInDBOptional.isPresent()) {
      infrastructureMappingRepository.save(infrastructureMapping);
    }

    return infrastructureMapping;
  }

  private DeploymentEvent prepareDeploymentEvent(
      OrchestrationEvent event, InfrastructureMapping infrastructureMapping) {
    Ambiance ambiance = event.getAmbiance();
    /**
     *   private String pipelineExecutionName;
     *   private String artifactId;
     *   private String artifactName;
     *   private String artifactBuildNum;
     *   private String deployedById;
     *   private String deployedByName;
     *
     *   // save both primary and sidecard artifacts
     */

    InstanceHandler instanceHandler = getInstanceHandler(ambiance);

    DeploymentSummary deploymentSummary = DeploymentSummary.builder()
                                              .accountIdentifier(AmbianceHelper.getAccountId(ambiance))
                                              .orgIdentifier(AmbianceHelper.getOrgIdentifier(ambiance))
                                              .projectIdentifier(AmbianceHelper.getProjectIdentifier(ambiance))
                                              .infrastructureMapping(infrastructureMapping)
                                              .infrastructureMappingId(infrastructureMapping.getId())
                                              .pipelineExecutionId(ambiance.getPlanExecutionId())
                                              .deploymentInfo(instanceHandler.getDeploymentInfo(ambiance))
                                              .deployedAt(event.getNodeExecutionProto().getEndTs().getSeconds())
                                              .build();

    // TODO fetch rollback info and set into deployment event

    return new DeploymentEvent(deploymentSummary, null);
  }

  private InstanceHandler getInstanceHandler(Ambiance ambiance) {
    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));

    return instanceHandlerFactoryService.getInstanceHandlerByType(infrastructureOutcome.getType());
  }
}
