package io.harness.cvng.statemachine.services;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator.AnalysisOrchestratorKeys;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.statemachine.entities.AnalysisStatus;
import io.harness.cvng.statemachine.exception.AnalysisOrchestrationException;
import io.harness.cvng.statemachine.services.intfc.AnalysisStateMachineService;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;

@Slf4j
public class OrchestrationServiceImpl implements OrchestrationService {
  @Inject HPersistence hPersistence;
  @Inject AnalysisStateMachineService stateMachineService;

  @Override
  public void queueAnalysis(String cvConfigId, Instant startTime, Instant endTime) {
    boolean isFirstAnalysis = false;
    AnalysisInput inputForAnalysis =
        AnalysisInput.builder().cvConfigId(cvConfigId).startTime(startTime).endTime(endTime).build();
    if (!validateAnalysisInputs(inputForAnalysis)) {
      throw new AnalysisOrchestrationException("Invalid Input while queuing analysis");
    }
    AnalysisOrchestrator orchestrator = getOrchestratorForCvConfig(cvConfigId);

    if (orchestrator == null) {
      orchestrator = AnalysisOrchestrator.builder()
                         .cvConfigId(inputForAnalysis.getCvConfigId())
                         .status(AnalysisStatus.CREATED)
                         .build();
      isFirstAnalysis = true;
    }
    AnalysisStateMachine stateMachine = stateMachineService.createStateMachine(inputForAnalysis);
    if (orchestrator.getAnalysisStateMachineQueue() == null) {
      orchestrator.setAnalysisStateMachineQueue(new ArrayList<>());
    }
    orchestrator.getAnalysisStateMachineQueue().add(stateMachine);
    hPersistence.save(orchestrator);
    if (isFirstAnalysis) {
      orchestrateNewAnalysisStateMachine(orchestrator);
    }
  }

  private boolean validateAnalysisInputs(AnalysisInput inputs) {
    boolean isValid = true;
    if (isEmpty(inputs.getCvConfigId()) || inputs.getStartTime() == null || inputs.getEndTime() == null) {
      isValid = false;
    } else {
      CVConfig cvConfig = hPersistence.get(CVConfig.class, inputs.getCvConfigId());
      if (cvConfig == null) {
        isValid = false;
      }
    }
    return isValid;
  }

  private AnalysisOrchestrator getOrchestratorForCvConfig(String cvConfigId) {
    return hPersistence.createQuery(AnalysisOrchestrator.class)
        .field(AnalysisOrchestratorKeys.cvConfigId)
        .equal(cvConfigId)
        .get();
  }

  @Override
  public void orchestrate(String cvConfigId) {
    Preconditions.checkNotNull(cvConfigId, "cvConfigId cannot be null when trying to orchestrate");
    logger.info("Orchestrating for config: {}", cvConfigId);
    AnalysisOrchestrator orchestrator = getOrchestratorForCvConfig(cvConfigId);
    orchestrateAtRunningState(orchestrator);
  }

  private void orchestrateAtRunningState(AnalysisOrchestrator orchestrator) {
    if (orchestrator == null) {
      String errMsg = "Invalid orchestrator in orchestrateAtRunningState. Returning";
      logger.error(errMsg);
      throw new AnalysisOrchestrationException(errMsg);
    }

    AnalysisStateMachine currentlyExecutingStateMachine =
        stateMachineService.getExecutingStateMachine(orchestrator.getCvConfigId());
    if (orchestrator.getStatus() == AnalysisStatus.CREATED) {
      currentlyExecutingStateMachine = orchestrator.getAnalysisStateMachineQueue().get(0);
    }
    switch (currentlyExecutingStateMachine.getStatus()) {
      case CREATED:
      case SUCCESS:
        orchestrateNewAnalysisStateMachine(orchestrator);
        break;
      case RUNNING:
        logger.info("For {}, state machine is currently RUNNING. "
                + "We will call executeStateMachine() to handover execution to state machine.",
            orchestrator.getCvConfigId());
        stateMachineService.executeStateMachine(currentlyExecutingStateMachine);
        break;
      case FAILED:
      case TIMEOUT:
        orchestrateFailedStateMachine(orchestrator, currentlyExecutingStateMachine);
        break;
      case COMPLETED:
        logger.info("Analysis for the entire duration is done. Time to close down");
        break;
      default:
        logger.info("Unknown analysis status of the state machine under execution");
    }
    hPersistence.save(orchestrator);
  }

  private void orchestrateFailedStateMachine(
      AnalysisOrchestrator orchestrator, AnalysisStateMachine currentStateMachine) {
    stateMachineService.retryStateMachineAfterFailure(currentStateMachine);
  }

  private void orchestrateNewAnalysisStateMachine(AnalysisOrchestrator orchestrator) {
    if (isNotEmpty(orchestrator.getAnalysisStateMachineQueue())) {
      AnalysisStateMachine analysisStateMachine = orchestrator.getAnalysisStateMachineQueue().remove(0);
      stateMachineService.initiateStateMachine(orchestrator.getCvConfigId(), analysisStateMachine);
      orchestrator.setStatus(AnalysisStatus.RUNNING);
      hPersistence.save(orchestrator);
    } else {
      logger.info("There is currently nothing new to analyze for cvConfig: {}", orchestrator.getCvConfigId());
    }
  }
}
