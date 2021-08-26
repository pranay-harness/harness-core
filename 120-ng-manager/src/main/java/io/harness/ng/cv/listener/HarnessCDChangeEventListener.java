package io.harness.ng.cv.listener;

import static io.harness.pms.contracts.execution.events.OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.service.steps.ServiceStepOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.cvng.beans.change.event.ChangeEventDTO;
import io.harness.cvng.beans.change.event.metadata.HarnessCDEventMetaData;
import io.harness.cvng.client.CVNGClient;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CV)
public class HarnessCDChangeEventListener implements OrchestrationEventHandler {
  private final static String DEPLOYMENT_STEP_TYPE = "DEPLOYMENT_STAGE_STEP";

  @Inject OutcomeService outcomeService;

  @Inject CVNGClient cvngClient;

  public static Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> getEngineEventHandlers() {
    Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> handlerMap = new HashMap<>();
    handlerMap.put(NODE_EXECUTION_STATUS_UPDATE, Sets.newHashSet(HarnessCDChangeEventListener.class));
    return handlerMap;
  }

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    StepType stepType = AmbianceUtils.getCurrentStepType(ambiance);
    if (!stepType.getType().equals(DEPLOYMENT_STEP_TYPE)) {
      return;
    }
    Optional<ServiceStepOutcome> optionalServiceStepOutcome = getServiceOutcomeFromAmbiance(ambiance);
    Optional<InfrastructureOutcome> optionalInfrastructureOutcome = getInfrastructureOutcomeFromAmbiance(ambiance);
    if (!optionalServiceStepOutcome.isPresent() || !optionalInfrastructureOutcome.isPresent()) {
      // TODO: remove this code once Environment details can be retrieved for all Status
      return;
    }
    registerChangeEvent(event, optionalServiceStepOutcome.get().getIdentifier(),
        optionalInfrastructureOutcome.get().getEnvironment().getIdentifier());
  }

  private void registerChangeEvent(OrchestrationEvent event, String serviceIdentifier, String environmentIdentifier) {
    ChangeEventDTO cdChangeEvent = getChangeEvent(event, serviceIdentifier, environmentIdentifier);
    try {
      cvngClient.registerChangeEvent(cdChangeEvent, AmbianceUtils.getAccountId(event.getAmbiance())).execute();
    } catch (IOException ex) {
      log.error("Exception while trying to register deployment change Event : " + cdChangeEvent, ex);
    }
  }

  private ChangeEventDTO getChangeEvent(
      OrchestrationEvent event, String serviceIdentifier, String environmentIdentifier) {
    Ambiance ambiance = event.getAmbiance();
    return ChangeEventDTO.builder()
        .accountId(AmbianceUtils.getAccountId(ambiance))
        .orgIdentifier(AmbianceUtils.getOrgIdentifier(ambiance))
        .projectIdentifier(AmbianceUtils.getProjectIdentifier(ambiance))
        .serviceIdentifier(serviceIdentifier)
        .envIdentifier(environmentIdentifier)
        .eventTime(Instant.now().getEpochSecond())
        .changeEventMetaData(
            HarnessCDEventMetaData.builder()
                .deploymentStartTime(AmbianceUtils.getCurrentLevelStartTs(event.getAmbiance()))
                .deploymentEndTime(StatusUtils.isFinalStatus(event.getStatus()) ? Instant.now().getEpochSecond() : null)
                .executionId(event.getAmbiance().getPlanExecutionId())
                .stageId(AmbianceUtils.getStageLevelFromAmbiance(event.getAmbiance()).get().getIdentifier())
                .status(event.getStatus())
                .build())
        .build();
  }

  private Optional<ServiceStepOutcome> getServiceOutcomeFromAmbiance(Ambiance ambiance) {
    OptionalOutcome optionalOutcome = outcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE));
    if (optionalOutcome.isFound()) {
      return Optional.of((ServiceStepOutcome) optionalOutcome.getOutcome());
    }
    return Optional.empty();
  }

  private Optional<InfrastructureOutcome> getInfrastructureOutcomeFromAmbiance(Ambiance ambiance) {
    OptionalOutcome optionalOutcome = outcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));
    if (optionalOutcome.isFound()) {
      return Optional.of((InfrastructureOutcome) optionalOutcome.getOutcome());
    }
    return Optional.empty();
  }
}
