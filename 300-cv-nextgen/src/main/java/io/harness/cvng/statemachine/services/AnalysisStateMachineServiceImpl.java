package io.harness.cvng.statemachine.services;

import static io.harness.cvng.CVConstants.STATE_MACHINE_IGNORE_MINUTES;
import static io.harness.cvng.CVConstants.STATE_MACHINE_IGNORE_MINUTES_FOR_DEMO;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.entities.HealthVerificationPeriod;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.ActivityVerificationState;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine.AnalysisStateMachineKeys;
import io.harness.cvng.statemachine.entities.CanaryTimeSeriesAnalysisState;
import io.harness.cvng.statemachine.entities.DeploymentLogClusterState;
import io.harness.cvng.statemachine.entities.PreDeploymentLogClusterState;
import io.harness.cvng.statemachine.entities.ServiceGuardLogClusterState;
import io.harness.cvng.statemachine.entities.ServiceGuardTimeSeriesAnalysisState;
import io.harness.cvng.statemachine.entities.TestTimeSeriesAnalysisState;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;
import io.harness.cvng.statemachine.services.intfc.AnalysisStateMachineService;
import io.harness.cvng.verificationjob.entities.HealthVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;

@Slf4j
public class AnalysisStateMachineServiceImpl implements AnalysisStateMachineService {
  @Inject private Injector injector;
  @Inject private HPersistence hPersistence;
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private Clock clock;

  @Override
  public void initiateStateMachine(String verificationTaskId, AnalysisStateMachine stateMachine) {
    Preconditions.checkNotNull(
        stateMachine, "Cannot initiate an empty state machine for config %s", verificationTaskId);
    Preconditions.checkNotNull(
        stateMachine.getCurrentState(), "No start state available in state machine for %s", verificationTaskId);

    Preconditions.checkNotNull(stateMachine.getCurrentState(),
        "The start state is null for a state machine"
            + "for config %s",
        verificationTaskId);

    AnalysisStateMachine executingStateMachine = getExecutingStateMachine(verificationTaskId);
    if (executingStateMachine != null) {
      if (executingStateMachine.getStatus() != AnalysisStatus.SUCCESS
          && executingStateMachine.getStatus() != AnalysisStatus.IGNORED) {
        throw new AnalysisStateMachineException(
            "There can be only one statemachine execution at a time for verificationTaskId: " + verificationTaskId);
      }
    }
    Optional<AnalysisStateMachine> ignoredStatemachine = ignoreOldStateMachine(stateMachine);
    if (ignoredStatemachine.isPresent()) {
      stateMachine = ignoredStatemachine.get();
    } else {
      stateMachine.setVerificationTaskId(verificationTaskId);
      stateMachine.setStatus(AnalysisStatus.RUNNING);
      injector.injectMembers(stateMachine.getCurrentState());
      stateMachine.getCurrentState().execute();
    }
    hPersistence.save(stateMachine);
  }

  @Override
  public void executeStateMachine(String verificationTaskId) {
    if (isEmpty(verificationTaskId)) {
      log.error("Empty cvConfigId in executeStateMachine");
      throw new AnalysisStateMachineException("Empty cvConfigId in executeStateMachine");
    }
    AnalysisStateMachine analysisStateMachine =
        hPersistence.createQuery(AnalysisStateMachine.class)
            .filter(AnalysisStateMachineKeys.verificationTaskId, verificationTaskId)
            .filter(AnalysisStateMachineKeys.status, AnalysisStatus.RUNNING)
            .get();

    if (analysisStateMachine == null) {
      log.info("There is currently no analysis running for cvConfigId: {}", verificationTaskId);
    } else {
      executeStateMachine(analysisStateMachine);
    }
  }

  @Override
  public Optional<AnalysisStateMachine> ignoreOldStateMachine(AnalysisStateMachine analysisStateMachine) {
    Instant instantForAnalysis = analysisStateMachine.getAnalysisEndTime();
    if (analysisStateMachine.getStatus() != AnalysisStatus.RUNNING
        && clock.instant()
               .minus(analysisStateMachine.getStateMachineIgnoreMinutes().intValue(), ChronoUnit.MINUTES)
               .isAfter(instantForAnalysis)) {
      log.info("The statemachine for {} and range {} to {} is before {} minutes. We will be ignoring it.",
          analysisStateMachine.getVerificationTaskId(), analysisStateMachine.getAnalysisStartTime(),
          analysisStateMachine.getAnalysisEndTime(), STATE_MACHINE_IGNORE_MINUTES);
      analysisStateMachine.setStatus(AnalysisStatus.IGNORED);
      return Optional.of(analysisStateMachine);
    }
    return Optional.empty();
  }

  @Override
  public AnalysisStatus executeStateMachine(AnalysisStateMachine analysisStateMachine) {
    log.info("Executing state machine {} for verificationTask {}", analysisStateMachine.getUuid(),
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
        log.info("Analysis is currently RUNNING for {} and analysis range {} to {}. We will return.",
            analysisStateMachine.getVerificationTaskId(), analysisStateMachine.getAnalysisStartTime(),
            analysisStateMachine.getAnalysisEndTime());
        nextState = currentState.handleRunning();
        break;
      case TRANSITION:
        log.info(
            "Analysis is currently in TRANSITION for {} and analysis range {} to {}. We will call handleTransition.",
            analysisStateMachine.getVerificationTaskId(), analysisStateMachine.getAnalysisStartTime(),
            analysisStateMachine.getAnalysisEndTime());
        nextState = currentState.handleTransition();
        break;
      case TIMEOUT:
        log.info("Analysis has TIMED OUT for {} and analysis range {} to {}. We will call handleTimeout.",
            analysisStateMachine.getVerificationTaskId(), analysisStateMachine.getAnalysisStartTime(),
            analysisStateMachine.getAnalysisEndTime());
        nextState = currentState.handleTimeout();
        break;
      case FAILED:
        log.info("Analysis has FAILED for {} and analysis range {} to {}. We will call handleFailure.",
            analysisStateMachine.getVerificationTaskId(), analysisStateMachine.getAnalysisStartTime(),
            analysisStateMachine.getAnalysisEndTime());
        nextState = currentState.handleFailure();
        break;
      case RETRY:
        log.info("Analysis is going to be RETRIED for {} and analysis range {} to {}. We will call handleRetry.",
            analysisStateMachine.getVerificationTaskId(), analysisStateMachine.getAnalysisStartTime(),
            analysisStateMachine.getAnalysisEndTime());
        nextState = currentState.handleRetry();
        break;
      case SUCCESS:
        nextState = currentState.handleSuccess();
        break;
      default:
        log.error("Unexpected state in analysis statemachine execution: " + status);
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
      log.info(
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
    return analysisStateMachine.getCurrentState().getStatus();
  }

  @Override
  public void retryStateMachineAfterFailure(AnalysisStateMachine analysisStateMachine) {
    Preconditions.checkNotNull(analysisStateMachine, "state machine is null while retrying");
    if (Instant.ofEpochMilli(analysisStateMachine.getNextAttemptTime()).isAfter(Instant.now())) {
      log.info("The next attempt time for the statemachine {} is {}. Not going to retry now",
          analysisStateMachine.getUuid(), analysisStateMachine.getNextAttemptTime());
      return;
    }
    log.info("Retrying state machine for cvConfig {}", analysisStateMachine.getVerificationTaskId());
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
  public AnalysisStateMachine getExecutingStateMachine(String verificationTaskId) {
    Preconditions.checkNotNull(
        verificationTaskId, "verificationTaskId is null when trying to query for executing state machine");
    return hPersistence.createQuery(AnalysisStateMachine.class)
        .filter(AnalysisStateMachineKeys.verificationTaskId, verificationTaskId)
        .order(Sort.descending(AnalysisStateMachineKeys.createdAt))
        .get();
  }

  @Override
  public void save(List<AnalysisStateMachine> stateMachineList) {
    if (isNotEmpty(stateMachineList)) {
      hPersistence.save(stateMachineList);
    }
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

    if (verificationTaskService.isServiceGuardId(inputForAnalysis.getVerificationTaskId())) {
      String cvConfigId = verificationTaskService.getCVConfigId(inputForAnalysis.getVerificationTaskId());
      CVConfig cvConfig = cvConfigService.get(cvConfigId);
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
      if (cvConfig.isDemo()) {
        stateMachine.setStateMachineIgnoreMinutes(STATE_MACHINE_IGNORE_MINUTES_FOR_DEMO);
      }
      firstState.setStatus(AnalysisStatus.CREATED);
      firstState.setInputs(inputForAnalysis);
      stateMachine.setAccountId(cvConfig.getAccountId());
      stateMachine.setCurrentState(firstState);
    } else {
      VerificationTask verificationTask = verificationTaskService.get(inputForAnalysis.getVerificationTaskId());
      VerificationJobInstance verificationJobInstance =
          verificationJobInstanceService.getVerificationJobInstance(verificationTask.getVerificationJobInstanceId());
      CVConfig cvConfigForDeployment = cvConfigService.get(verificationTask.getCvConfigId());
      Preconditions.checkNotNull(verificationJobInstance, "verificationJobInstance can not be null");
      Preconditions.checkNotNull(cvConfigForDeployment, "cvConfigForDeployment can not be null");
      stateMachine.setAccountId(verificationTask.getAccountId());
      if (verificationJobInstance.getResolvedJob().getType() == VerificationJobType.HEALTH) {
        createHealthAnalysisState(stateMachine, inputForAnalysis, verificationJobInstance);
      } else {
        createDeploymentAnalysisState(stateMachine, inputForAnalysis, verificationJobInstance, cvConfigForDeployment);
      }
    }
    return stateMachine;
  }

  private void createHealthAnalysisState(AnalysisStateMachine stateMachine, AnalysisInput inputForAnalysis,
      VerificationJobInstance verificationJobInstance) {
    HealthVerificationJob resolvedJob = (HealthVerificationJob) verificationJobInstance.getResolvedJob();
    ActivityVerificationState healthAnalysisState = ActivityVerificationState.builder().build();
    healthAnalysisState.setInputs(inputForAnalysis);
    healthAnalysisState.setHealthVerificationPeriod(HealthVerificationPeriod.PRE_ACTIVITY);
    healthAnalysisState.setDuration(verificationJobInstance.getResolvedJob().getDuration());
    healthAnalysisState.setPreActivityVerificationStartTime(
        resolvedJob.getPreActivityVerificationStartTime(verificationJobInstance.getStartTime()));
    healthAnalysisState.setPostActivityVerificationStartTime(
        resolvedJob.getPostActivityVerificationStartTime(verificationJobInstance.getStartTime()));
    healthAnalysisState.setStatus(AnalysisStatus.CREATED);
    stateMachine.setCurrentState(healthAnalysisState);
  }

  private void createDeploymentAnalysisState(AnalysisStateMachine stateMachine, AnalysisInput inputForAnalysis,
      VerificationJobInstance verificationJobInstance, CVConfig cvConfigForDeployment) {
    switch (cvConfigForDeployment.getVerificationType()) {
      case TIME_SERIES:
        if (verificationJobInstance.getResolvedJob().getType() == VerificationJobType.TEST) {
          TestTimeSeriesAnalysisState testTimeSeriesAnalysisState = TestTimeSeriesAnalysisState.builder().build();
          testTimeSeriesAnalysisState.setStatus(AnalysisStatus.CREATED);
          testTimeSeriesAnalysisState.setInputs(inputForAnalysis);
          stateMachine.setCurrentState(testTimeSeriesAnalysisState);
        } else {
          CanaryTimeSeriesAnalysisState canaryTimeSeriesAnalysisState = CanaryTimeSeriesAnalysisState.builder().build();
          canaryTimeSeriesAnalysisState.setStatus(AnalysisStatus.CREATED);
          canaryTimeSeriesAnalysisState.setInputs(inputForAnalysis);
          stateMachine.setCurrentState(canaryTimeSeriesAnalysisState);
        }
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

  private AnalysisState createDeploymentLogState(
      AnalysisInput analysisInput, VerificationJobInstance verificationJobInstance) {
    Optional<TimeRange> preDeploymentTimeRange =
        verificationJobInstanceService.getPreDeploymentTimeRange(verificationJobInstance.getUuid());
    if (preDeploymentTimeRange.isPresent() && preDeploymentTimeRange.get().equals(analysisInput.getTimeRange())) {
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
