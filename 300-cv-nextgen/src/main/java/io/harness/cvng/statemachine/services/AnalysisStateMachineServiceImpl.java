package io.harness.cvng.statemachine.services;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine.AnalysisStateMachineKeys;
import io.harness.cvng.statemachine.entities.CanaryTimeSeriesAnalysisState;
import io.harness.cvng.statemachine.entities.DeploymentLogClusterState;
import io.harness.cvng.statemachine.entities.PreDeploymentLogClusterState;
import io.harness.cvng.statemachine.entities.ServiceGuardLogClusterState;
import io.harness.cvng.statemachine.entities.ServiceGuardTimeSeriesAnalysisState;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;
import io.harness.cvng.statemachine.services.intfc.AnalysisStateMachineService;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Slf4j
public class AnalysisStateMachineServiceImpl implements AnalysisStateMachineService {
  @Inject private Injector injector;
  @Inject private HPersistence hPersistence;
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private VerificationJobService verificationJobService;

  @Override
  public void initiateStateMachine(String cvConfigId, AnalysisStateMachine stateMachine) {
    Preconditions.checkNotNull(stateMachine, "Cannot initiate an empty state machine for config %s", cvConfigId);
    Preconditions.checkNotNull(
        stateMachine.getCurrentState(), "No start state available in state machine for %s", cvConfigId);

    Preconditions.checkNotNull(stateMachine.getCurrentState(),
        "The start state is null for a state machine"
            + "for config %s",
        cvConfigId);

    AnalysisStateMachine executingStateMachine = getExecutingStateMachine(cvConfigId);
    if (executingStateMachine != null) {
      if (executingStateMachine.getStatus() != AnalysisStatus.SUCCESS) {
        throw new AnalysisStateMachineException(
            "There can be only one statemachine execution at a time for cvConfig: " + cvConfigId);
      }
    }
    stateMachine.setCvConfigId(cvConfigId);
    stateMachine.setVerificationTaskId(cvConfigId);
    stateMachine.setStatus(AnalysisStatus.RUNNING);
    injector.injectMembers(stateMachine.getCurrentState());
    stateMachine.getCurrentState().execute();
    hPersistence.save(stateMachine);
  }

  @Override
  public void executeStateMachine(String cvConfigId) {
    if (isEmpty(cvConfigId)) {
      logger.error("Empty cvConfigId in executeStateMachine");
      throw new AnalysisStateMachineException("Empty cvConfigId in executeStateMachine");
    }
    AnalysisStateMachine analysisStateMachine = hPersistence.createQuery(AnalysisStateMachine.class)
                                                    .filter(AnalysisStateMachineKeys.cvConfigId, cvConfigId)
                                                    .filter(AnalysisStateMachineKeys.status, AnalysisStatus.RUNNING)
                                                    .get();

    if (analysisStateMachine == null) {
      logger.info("There is currently no analysis running for cvConfigId: {}", cvConfigId);
    } else {
      executeStateMachine(analysisStateMachine);
    }
  }

  @Override
  public void executeStateMachine(AnalysisStateMachine analysisStateMachine) {
    Instant instantForAnalysis = analysisStateMachine.getAnalysisEndTime();
    if (analysisStateMachine.getStatus() != AnalysisStatus.RUNNING
        && Instant.now().minus(2, ChronoUnit.HOURS).isAfter(instantForAnalysis)) {
      logger.info("The statemachine for {} and range {} to {} is before 2 hours. We will be ignoring it.",
          analysisStateMachine.getVerificationTaskId(), analysisStateMachine.getAnalysisStartTime(),
          analysisStateMachine.getAnalysisEndTime());
      analysisStateMachine.setStatus(AnalysisStatus.IGNORED);
      return;
    }

    logger.info("Executing state machine {} for verificationTask {}", analysisStateMachine.getUuid(),
        analysisStateMachine.getVerificationTaskId());

    AnalysisState currentState = analysisStateMachine.getCurrentState();
    injector.injectMembers(currentState);
    AnalysisStatus status = currentState.getExecutionStatus();
    AnalysisState nextState = null;
    switch (status) {
      case CREATED:
        nextState = currentState.execute();
        break;
      case RUNNING:
        logger.info("Analysis is currently RUNNING for {} and analysis range {} to {}. We will return.",
            analysisStateMachine.getVerificationTaskId(), analysisStateMachine.getAnalysisStartTime(),
            analysisStateMachine.getAnalysisEndTime());
        return;
      case TRANSITION:
        logger.info(
            "Analysis is currently in TRANSITION for {} and analysis range {} to {}. We will call handleTransition.",
            analysisStateMachine.getVerificationTaskId(), analysisStateMachine.getAnalysisStartTime(),
            analysisStateMachine.getAnalysisEndTime());
        nextState = currentState.handleTransition();
        break;
      case TIMEOUT:
        logger.info("Analysis has TIMED OUT for {} and analysis range {} to {}. We will call handleTimeout.",
            analysisStateMachine.getVerificationTaskId(), analysisStateMachine.getAnalysisStartTime(),
            analysisStateMachine.getAnalysisEndTime());
        nextState = currentState.handleTimeout();
        break;
      case FAILED:
        logger.info("Analysis has FAILED for {} and analysis range {} to {}. We will call handleFailure.",
            analysisStateMachine.getVerificationTaskId(), analysisStateMachine.getAnalysisStartTime(),
            analysisStateMachine.getAnalysisEndTime());
        nextState = currentState.handleFailure();
        break;
      case RETRY:
        logger.info("Analysis is going to be RETRIED for {} and analysis range {} to {}. We will call handleRetry.",
            analysisStateMachine.getVerificationTaskId(), analysisStateMachine.getAnalysisStartTime(),
            analysisStateMachine.getAnalysisEndTime());
        nextState = currentState.handleRetry();
        break;
      case SUCCESS:
        nextState = currentState.handleSuccess();
        break;
      default:
        logger.error("Unexpected state in analysis statemachine execution: " + status);
        throw new AnalysisStateMachineException("Unexpected state in analysis statemachine execution: " + status);
    }
    AnalysisState previousState = analysisStateMachine.getCurrentState();
    analysisStateMachine.setCurrentState(nextState);
    injector.injectMembers(nextState);
    if (AnalysisStatus.getFinalStates().contains(nextState.getStatus())) {
      analysisStateMachine.setStatus(nextState.getStatus());
    } else if (nextState.getStatus() == AnalysisStatus.CREATED) {
      if (analysisStateMachine.getCompletedStates() == null) {
        analysisStateMachine.setCompletedStates(new ArrayList<>());
      }
      analysisStateMachine.getCompletedStates().add(previousState);
      nextState.execute();
    }
    if (nextState.getStatus() == AnalysisStatus.SUCCESS) {
      // the state machine is done, time to mark it as success
      logger.info(
          "Analysis state machine has completed successfully for verificationTaskId: {} and analysis range {} to {}",
          analysisStateMachine.getVerificationTaskId(), analysisStateMachine.getAnalysisStartTime(),
          analysisStateMachine.getAnalysisEndTime());
      analysisStateMachine.setStatus(AnalysisStatus.SUCCESS);
      nextState.handleFinalStatuses(nextState.getStatus());
    } else if (AnalysisStatus.getFinalStates().contains(nextState.getStatus())) {
      // The current state has closed down as either FAILED or TIMEOUT
      analysisStateMachine.setNextAttemptTime(Instant.now().plus(30, ChronoUnit.MINUTES).toEpochMilli());
      analysisStateMachine.setStatus(nextState.getStatus());
      nextState.handleFinalStatuses(nextState.getStatus());
    }
    hPersistence.save(analysisStateMachine);
  }

  @Override
  public void retryStateMachineAfterFailure(AnalysisStateMachine analysisStateMachine) {
    Preconditions.checkNotNull(analysisStateMachine, "state machine is null while retrying");
    if (Instant.ofEpochMilli(analysisStateMachine.getNextAttemptTime()).isAfter(Instant.now())) {
      logger.info("The next attempt time for the statemachine {} is {}. Not going to retry now",
          analysisStateMachine.getUuid(), analysisStateMachine.getNextAttemptTime());
      return;
    }
    logger.info("Retrying state machine for cvConfig {}", analysisStateMachine.getCvConfigId());
    AnalysisState currentState = analysisStateMachine.getCurrentState();
    injector.injectMembers(currentState);
    if (currentState.getStatus() == AnalysisStatus.FAILED || currentState.getStatus() == AnalysisStatus.TIMEOUT) {
      analysisStateMachine.setStatus(AnalysisStatus.RUNNING);

      currentState.handleRerun();

    } else {
      throw new AnalysisStateMachineException(
          "Attempting to retry state machine after failure when current status is not failed");
    }

    hPersistence.save(analysisStateMachine);
  }

  @Override
  public AnalysisStateMachine getExecutingStateMachine(String cvConfigId) {
    Preconditions.checkNotNull(cvConfigId, "cvConfigId is null when trying to query for executing state machine");
    return hPersistence.createQuery(AnalysisStateMachine.class)
        .filter(AnalysisStateMachineKeys.cvConfigId, cvConfigId)
        .order(Sort.descending(AnalysisStateMachineKeys.createdAt))
        .get();
  }

  @Override
  public AnalysisStateMachine createStateMachine(AnalysisInput inputForAnalysis) {
    // TODO: refactor this to make this polymorphic.
    AnalysisStateMachine stateMachine = AnalysisStateMachine.builder()
                                            .verificationTaskId(inputForAnalysis.getVerificationTaskId())
                                            .analysisStartTime(inputForAnalysis.getStartTime())
                                            .analysisEndTime(inputForAnalysis.getEndTime())
                                            .status(AnalysisStatus.CREATED)
                                            .build();

    CVConfig cvConfig = cvConfigService.get(inputForAnalysis.getVerificationTaskId());

    if (cvConfig != null) {
      VerificationType verificationType = cvConfig.getVerificationType();
      AnalysisState firstState = null;
      switch (verificationType) {
        case TIME_SERIES:
          firstState = ServiceGuardTimeSeriesAnalysisState.builder().build();
          break;
        case LOG:
          firstState = ServiceGuardLogClusterState.builder().clusterLevel(LogClusterLevel.L1).build();
          break;
        default:
          throw new AnalysisStateMachineException(
              "Unimplemented verification type for orchestration : " + verificationType);
      }
      firstState.setStatus(AnalysisStatus.CREATED);
      firstState.setInputs(inputForAnalysis);
      stateMachine.setCurrentState(firstState);
    } else {
      VerificationTask verificationTask = verificationTaskService.get(inputForAnalysis.getVerificationTaskId());
      VerificationJobInstance verificationJobInstance =
          verificationJobInstanceService.getVerificationJobInstance(verificationTask.getVerificationJobInstanceId());
      CVConfig cvConfigForDeployment = cvConfigService.get(verificationTask.getCvConfigId());
      Preconditions.checkNotNull(verificationJobInstance, "verificationJobInstance can not be null");
      Preconditions.checkNotNull(cvConfigForDeployment, "cvConfigForDeployment can not be null");

      switch (cvConfigForDeployment.getVerificationType()) {
        case TIME_SERIES:
          CanaryTimeSeriesAnalysisState canaryTimeSeriesAnalysisState = CanaryTimeSeriesAnalysisState.builder().build();
          canaryTimeSeriesAnalysisState.setStatus(AnalysisStatus.CREATED);
          canaryTimeSeriesAnalysisState.setInputs(inputForAnalysis);
          stateMachine.setCurrentState(canaryTimeSeriesAnalysisState);
          break;
        case LOG:
          AnalysisState analysisState = createDeploymentLogState(inputForAnalysis, verificationJobInstance);
          analysisState.setStatus(AnalysisStatus.CREATED);
          analysisState.setInputs(inputForAnalysis);
          stateMachine.setCurrentState(analysisState);
          break;
        default:
          throw new IllegalStateException("Invalid verificationType");
      }
    }
    return stateMachine;
  }

  private AnalysisState createDeploymentLogState(
      AnalysisInput analysisInput, VerificationJobInstance verificationJobInstance) {
    TimeRange preDeploymentTimeRange =
        verificationJobInstanceService.getPreDeploymentTimeRange(verificationJobInstance.getUuid());
    if (preDeploymentTimeRange.equals(analysisInput.getTimeRange())) {
      // first task so needs to enqueue clustering task
      PreDeploymentLogClusterState preDeploymentLogClusterState = PreDeploymentLogClusterState.builder().build();
      preDeploymentLogClusterState.setClusterLevel(LogClusterLevel.L1);
      return preDeploymentLogClusterState;
    } else {
      DeploymentLogClusterState deploymentLogClusterState = DeploymentLogClusterState.builder().build();
      deploymentLogClusterState.setClusterLevel(LogClusterLevel.L1);
      return deploymentLogClusterState;
    }
  }
}
