package io.harness.cvng.statemachine.services;

import static io.harness.cvng.CVConstants.STATE_MACHINE_IGNORE_LIMIT;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class OrchestrationServiceImpl implements OrchestrationService {
  @Inject private HPersistence hPersistence;
  @Inject private AnalysisStateMachineService stateMachineService;

  @Override
  public void queueAnalysis(String verificationTaskId, Instant startTime, Instant endTime) {
    log.info("Queuing analysis for verificationTaskId: {}, startTime: {}, endTime: {}", verificationTaskId, startTime,
        endTime);

    AnalysisInput inputForAnalysis =
        AnalysisInput.builder().verificationTaskId(verificationTaskId).startTime(startTime).endTime(endTime).build();
    validateAnalysisInputs(inputForAnalysis);

    AnalysisStateMachine stateMachine = stateMachineService.createStateMachine(inputForAnalysis);

    Query<AnalysisOrchestrator> orchestratorQuery =
        hPersistence.createQuery(AnalysisOrchestrator.class)
            .filter(AnalysisOrchestratorKeys.verificationTaskId, verificationTaskId);

    UpdateOperations<AnalysisOrchestrator> updateOperations =
        hPersistence.createUpdateOperations(AnalysisOrchestrator.class)
            .setOnInsert(AnalysisOrchestratorKeys.verificationTaskId, verificationTaskId)
            .setOnInsert(AnalysisOrchestratorKeys.status, AnalysisStatus.CREATED)
            .addToSet(AnalysisOrchestratorKeys.analysisStateMachineQueue, Arrays.asList(stateMachine));

    hPersistence.upsert(orchestratorQuery, updateOperations);
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
    try {
      orchestrateAtRunningState(orchestrator);
    } catch (Exception e) {
      // TODO: these errors needs to go to execution log so that we can connect it with the right context and show them
      // in the UI.
      // TODO: setup alert
      log.error("Failed to orchestrate: {}", orchestrator, e);
      throw e;
    }
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

    AnalysisStatus stateMachineStatus = null;
    switch (currentlyExecutingStateMachine.getStatus()) {
      case CREATED:
      case SUCCESS:
      case IGNORED:
        orchestrateNewAnalysisStateMachine(orchestrator.getVerificationTaskId());
        break;
      case RUNNING:
        log.info("For {}, state machine is currently RUNNING. "
                + "We will call executeStateMachine() to handover execution to state machine.",
            orchestrator.getVerificationTaskId());
        stateMachineStatus = stateMachineService.executeStateMachine(currentlyExecutingStateMachine);
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
    if (AnalysisStatus.SUCCESS == stateMachineStatus || AnalysisStatus.COMPLETED == stateMachineStatus) {
      orchestrateNewAnalysisStateMachine(orchestrator.getVerificationTaskId());
    }
  }

  private void orchestrateFailedStateMachine(AnalysisStateMachine currentStateMachine) {
    stateMachineService.retryStateMachineAfterFailure(currentStateMachine);
  }

  private AnalysisStateMachine getFrontOfStateMachineQueue(String verificationTaskId) {
    Query<AnalysisOrchestrator> orchestratorQuery =
        hPersistence.createQuery(AnalysisOrchestrator.class)
            .filter(AnalysisOrchestratorKeys.verificationTaskId, verificationTaskId);
    UpdateOperations<AnalysisOrchestrator> updateOperations =
        hPersistence.createUpdateOperations(AnalysisOrchestrator.class)
            .removeFirst(AnalysisOrchestratorKeys.analysisStateMachineQueue);
    AnalysisOrchestrator orchestrator =
        hPersistence.findAndModify(orchestratorQuery, updateOperations, new FindAndModifyOptions().returnNew(false));

    return orchestrator.getAnalysisStateMachineQueue() != null && orchestrator.getAnalysisStateMachineQueue().size() > 0
        ? orchestrator.getAnalysisStateMachineQueue().get(0)
        : null;
  }

  private void orchestrateNewAnalysisStateMachine(String verificationTaskId) {
    int ignoredCount = 0;
    List<AnalysisStateMachine> ignoredStatemachines = new ArrayList<>();
    AnalysisStateMachine analysisStateMachine = null;
    while (ignoredCount < STATE_MACHINE_IGNORE_LIMIT) {
      analysisStateMachine = getFrontOfStateMachineQueue(verificationTaskId);

      if (analysisStateMachine == null) {
        log.info("There is currently nothing to analyze for verificationTaskId {}", verificationTaskId);
        return;
      }

      Optional<AnalysisStateMachine> ignoredStateMachine = analysisStateMachine == null
          ? Optional.empty()
          : stateMachineService.ignoreOldStateMachine(analysisStateMachine);
      if (!ignoredStateMachine.isPresent()) {
        break;
      }
      ignoredCount++;

      // add to ignored list and remove from queue
      ignoredStatemachines.add(ignoredStateMachine.get());
    }
    if (ignoredCount > 0) {
      // save all the ignored state machines.
      stateMachineService.save(ignoredStatemachines);
    }

    if (analysisStateMachine != null && ignoredCount < STATE_MACHINE_IGNORE_LIMIT) {
      stateMachineService.initiateStateMachine(verificationTaskId, analysisStateMachine);
    }

    updateStatusOfOrchestrator(verificationTaskId, AnalysisStatus.RUNNING);
  }

  private void updateStatusOfOrchestrator(String verificationTaskId, AnalysisStatus status) {
    Query<AnalysisOrchestrator> orchestratorQuery =
        hPersistence.createQuery(AnalysisOrchestrator.class)
            .filter(AnalysisOrchestratorKeys.verificationTaskId, verificationTaskId);

    UpdateOperations<AnalysisOrchestrator> updateOperations =
        hPersistence.createUpdateOperations(AnalysisOrchestrator.class).set(AnalysisOrchestratorKeys.status, status);

    hPersistence.update(orchestratorQuery, updateOperations);
  }
}
