package io.harness.cvng.statemachine.services.api;

import io.harness.cvng.analysis.entities.HealthVerificationPeriod;
import io.harness.cvng.analysis.services.api.HealthVerificationService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.ActivityVerificationState;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import lombok.Builder;

public class ActivityVerificationStateExecutor extends AnalysisStateExecutor<ActivityVerificationState> {
  @Inject private transient HealthVerificationService healthVerificationService;
  private Instant preActivityVerificationStartTime;
  private Instant postActivityVerificationStartTime;

  private String duration;

  @Builder.Default private Instant analysisCompletedUntil = Instant.ofEpochMilli(0);

  // pre vs post Activity
  private HealthVerificationPeriod healthVerificationPeriod;

  public Duration getDurationObj() {
    return Duration.parse(duration);
  }

  public void setDuration(Duration duration) {
    this.duration = duration.toString();
  }

  private boolean isAllAnalysesComplete(ActivityVerificationState analysisState) {
    boolean shouldTransition = true;
    if (analysisCompletedUntil.isBefore(analysisState.getInputs().getEndTime())) {
      shouldTransition = false;
    }
    return shouldTransition;
  }

  @Override
  public AnalysisState execute(ActivityVerificationState analysisState) {
    analysisCompletedUntil = healthVerificationService.aggregateActivityAnalysis(
        analysisState.getInputs().getVerificationTaskId(), analysisState.getInputs().getStartTime(),
        analysisState.getInputs().getEndTime(), analysisCompletedUntil, healthVerificationPeriod);
    analysisState.setStatus(AnalysisStatus.RUNNING);
    return analysisState;
  }

  @Override
  public AnalysisStatus getExecutionStatus(ActivityVerificationState analysisState) {
    if (isAllAnalysesComplete(analysisState)
        && healthVerificationPeriod.equals(HealthVerificationPeriod.PRE_ACTIVITY)) {
      analysisState.setStatus(AnalysisStatus.TRANSITION);
    } else if (isAllAnalysesComplete(analysisState)
        && healthVerificationPeriod.equals(HealthVerificationPeriod.POST_ACTIVITY)) {
      analysisState.setStatus(AnalysisStatus.SUCCESS);
    }
    if (analysisCompletedUntil.toEpochMilli() != 0) {
      healthVerificationService.updateProgress(
          analysisState.getInputs().getVerificationTaskId(), analysisCompletedUntil, analysisState.getStatus(), false);
    }
    // TODO: When should we mark this as failed/error ?
    // TODO: What to do if the analysis had stopped for some reason for one of the underlying configs.
    return analysisState.getStatus();
  }

  @Override
  public AnalysisState handleRerun(ActivityVerificationState analysisState) {
    return null;
  }

  @Override
  public AnalysisState handleRunning(ActivityVerificationState analysisState) {
    // update any new analyses if possible.
    analysisCompletedUntil = healthVerificationService.aggregateActivityAnalysis(
        analysisState.getInputs().getVerificationTaskId(), analysisState.getInputs().getStartTime(),
        analysisState.getInputs().getEndTime(), analysisCompletedUntil, healthVerificationPeriod);
    return analysisState;
  }

  @Override
  public AnalysisState handleSuccess(ActivityVerificationState analysisState) {
    analysisState.setStatus(AnalysisStatus.COMPLETED);
    healthVerificationService.updateProgress(
        analysisState.getInputs().getVerificationTaskId(), analysisCompletedUntil, analysisState.getStatus(), true);
    return analysisState;
  }

  @Override
  public AnalysisState handleTransition(ActivityVerificationState analysisState) {
    switch (healthVerificationPeriod) {
      case PRE_ACTIVITY:
        ActivityVerificationState postActivityVerificationState =
            ActivityVerificationState.builder()
                .preActivityVerificationStartTime(preActivityVerificationStartTime)
                .postActivityVerificationStartTime(postActivityVerificationStartTime)
                .healthVerificationPeriod(HealthVerificationPeriod.POST_ACTIVITY)
                .build();
        AnalysisInput input = AnalysisInput.builder()
                                  .verificationTaskId(analysisState.getInputs().getVerificationTaskId())
                                  .startTime(postActivityVerificationStartTime)
                                  .endTime(postActivityVerificationStartTime.plus(getDurationObj()))
                                  .build();
        postActivityVerificationState.setInputs(input);
        postActivityVerificationState.setStatus(AnalysisStatus.CREATED);
        return postActivityVerificationState;

      case POST_ACTIVITY:
        analysisState.setStatus(AnalysisStatus.SUCCESS);
        return analysisState;
      default:
        throw new AnalysisStateMachineException("Unknown period in health verification");
    }
  }

  @Override
  public AnalysisState handleRetry(ActivityVerificationState analysisState) {
    return null;
  }
}
