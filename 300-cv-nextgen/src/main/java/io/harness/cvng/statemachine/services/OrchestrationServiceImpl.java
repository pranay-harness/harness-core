package io.harness.cvng.statemachine.services;

import static io.harness.cvng.CVConstants.STATE_MACHINE_IGNORE_LIMIT;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator.AnalysisOrchestratorKeys;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.statemachine.services.intfc.AnalysisStateMachineService;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrchestrationServiceImpl implements OrchestrationService {
  @Inject private HPersistence hPersistence;
  @Inject private AnalysisStateMachineService stateMachineService;
  @Inject private VerificationTaskService verificationTaskService;

  @Override
  public void queueAnalysis(String verificationTaskId, Instant startTime, Instant endTime) {
    boolean isFirstAnalysis = false;
    AnalysisInput inputForAnalysis =
        AnalysisInput.builder().verificationTaskId(verificationTaskId).startTime(startTime).endTime(endTime).build();
    validateAnalysisInputs(inputForAnalysis);

    AnalysisOrchestrator orchestrator = getOrchestrator(verificationTaskId);

    if (orchestrator == null) {
      orchestrator = AnalysisOrchestrator.builder()
                         .verificationTaskId(inputForAnalysis.getVerificationTaskId())
                         .status(AnalysisStatus.CREATED)
                         .build();
      isFirstAnalysis = true;
    }
    AnalysisStateMachine stateMachine = stateMachineService.createStateMachine(inputForAnalysis);
    // TODO: can we move it inside null if condition?
    if (orchestrator.getAnalysisStateMachineQueue() == null) {
      orchestrator.setAnalysisStateMachineQueue(new ArrayList<>());
    }
    orchestrator.getAnalysisStateMachineQueue().add(stateMachine);
    hPersistence.save(orchestrator);
    if (isFirstAnalysis) {
      orchestrateNewAnalysisStateMachine(orchestrator);
    }
  }

  private void validateAnalysisInputs(AnalysisInput inputs) {
    Preconditions.checkNotNull(inputs.getVerificationTaskId(), "verificationTaskId can not be null");
    Preconditions.checkNotNull(inputs.getStartTime(), "startTime can not be null");
    Preconditions.checkNotNull(inputs.getEndTime(), "endTime can not be null");
  }

  private AnalysisOrchestrator getOrchestrator(String verificationTaskId) {
    return hPersistence.createQuery(AnalysisOrchestrator.class)
        .field(AnalysisOrchestratorKeys.verificationTaskId)
        .equal(verificationTaskId)
        .get();
  }

  @Override
  public void orchestrate(String verificationTaskId) {
    Preconditions.checkNotNull(verificationTaskId, "verificationTaskId cannot be null when trying to orchestrate");
    log.info("Orchestrating for verificationTaskId: {}", verificationTaskId);
    AnalysisOrchestrator orchestrator = getOrchestrator(verificationTaskId);
    orchestrateAtRunningState(orchestrator);
  }

  @Override
  public void orchestrate(AnalysisOrchestrator orchestrator) {
    Preconditions.checkNotNull(orchestrator, "orchestrator cannot be null when trying to orchestrate");
    orchestrateAtRunningState(orchestrator);
  }

  private void orchestrateAtRunningState(AnalysisOrchestrator orchestrator) {
    if (orchestrator == null) {
      String errMsg = "No orchestrator available to execute currently.";
      log.info(errMsg);
      return;
    }

    if (isNotEmpty(orchestrator.getAnalysisStateMachineQueue())
        && orchestrator.getAnalysisStateMachineQueue().size() > 5) {
      log.info("For verification task ID {}, orchestrator has more than 5 tasks waiting."
              + " Please check if there is a growing backlog.",
          orchestrator.getVerificationTaskId());
    }

    AnalysisStateMachine currentlyExecutingStateMachine =
        stateMachineService.getExecutingStateMachine(orchestrator.getVerificationTaskId());
    if (orchestrator.getStatus() == AnalysisStatus.CREATED) {
      currentlyExecutingStateMachine = orchestrator.getAnalysisStateMachineQueue().get(0);
    }
    switch (currentlyExecutingStateMachine.getStatus()) {
      case CREATED:
      case SUCCESS:
      case IGNORED:
        orchestrateNewAnalysisStateMachine(orchestrator);
        break;
      case RUNNING:
        log.info("For {}, state machine is currently RUNNING. "
                + "We will call executeStateMachine() to handover execution to state machine.",
            orchestrator.getVerificationTaskId());
        stateMachineService.executeStateMachine(currentlyExecutingStateMachine);
        break;
      case FAILED:
      case TIMEOUT:
        orchestrateFailedStateMachine(currentlyExecutingStateMachine);
        break;
      case COMPLETED:
        log.info("Analysis for the entire duration is done. Time to close down");
        orchestrator.setStatus(AnalysisStatus.COMPLETED);
        break;
      default:
        log.info("Unknown analysis status of the state machine under execution");
    }
    hPersistence.save(orchestrator);
  }

  private void orchestrateFailedStateMachine(AnalysisStateMachine currentStateMachine) {
    stateMachineService.retryStateMachineAfterFailure(currentStateMachine);
  }

  private AnalysisStateMachine getFrontOfStateMachineQueue(AnalysisOrchestrator orchestrator) {
    return orchestrator.getAnalysisStateMachineQueue() != null && orchestrator.getAnalysisStateMachineQueue().size() > 0
        ? orchestrator.getAnalysisStateMachineQueue().get(0)
        : null;
  }

  private void orchestrateNewAnalysisStateMachine(AnalysisOrchestrator orchestrator) {
    if (isNotEmpty(orchestrator.getAnalysisStateMachineQueue())) {
      AnalysisStateMachine analysisStateMachine = getFrontOfStateMachineQueue(orchestrator);

      Optional<AnalysisStateMachine> ignoredStateMachine = analysisStateMachine == null
          ? Optional.empty()
          : stateMachineService.ignoreOldStatemachine(analysisStateMachine);
      int ignoredCount = 0;
      List<AnalysisStateMachine> ignoredStatemachines = new ArrayList<>();

      while (ignoredStateMachine.isPresent() && ignoredCount < STATE_MACHINE_IGNORE_LIMIT) {
        ignoredCount++;
        // add to ignored list and remove from queue
        ignoredStatemachines.add(ignoredStateMachine.get());
        orchestrator.getAnalysisStateMachineQueue().remove(0);

        analysisStateMachine = getFrontOfStateMachineQueue(orchestrator);
        ignoredStateMachine = analysisStateMachine == null
            ? Optional.empty()
            : stateMachineService.ignoreOldStatemachine(analysisStateMachine);
      }
      if (ignoredCount > 0) {
        // save all the ignored state machines.
        stateMachineService.save(ignoredStatemachines);
      }

      if (analysisStateMachine != null && ignoredCount < STATE_MACHINE_IGNORE_LIMIT) {
        stateMachineService.initiateStateMachine(orchestrator.getVerificationTaskId(), analysisStateMachine);
        orchestrator.getAnalysisStateMachineQueue().remove(0);
      }
      orchestrator.setStatus(AnalysisStatus.RUNNING);
      hPersistence.save(orchestrator);
    } else {
      log.info("There is currently nothing new to analyze for cvConfig: {}", orchestrator.getVerificationTaskId());
    }
  }
}
