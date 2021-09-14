/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.entities;

import io.harness.cvng.analysis.entities.HealthVerificationPeriod;
import io.harness.cvng.analysis.services.api.HealthVerificationService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
// TODO: rename this to HealthVerificationState, this will also require thinking about migration as the classpath is
// saved in db
public class ActivityVerificationState extends AnalysisState {
  @JsonIgnore @Inject private transient HealthVerificationService healthVerificationService;
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

  @Override
  public AnalysisState execute() {
    analysisCompletedUntil = healthVerificationService.aggregateActivityAnalysis(getInputs().getVerificationTaskId(),
        getInputs().getStartTime(), getInputs().getEndTime(), analysisCompletedUntil, healthVerificationPeriod);
    this.setStatus(AnalysisStatus.RUNNING);
    return this;
  }

  private boolean isAllAnalysesComplete() {
    boolean shouldTransition = true;
    if (analysisCompletedUntil.isBefore(getInputs().getEndTime())) {
      shouldTransition = false;
    }
    return shouldTransition;
  }

  @Override
  public AnalysisStatus getExecutionStatus() {
    if (isAllAnalysesComplete() && healthVerificationPeriod.equals(HealthVerificationPeriod.PRE_ACTIVITY)) {
      this.setStatus(AnalysisStatus.TRANSITION);
    } else if (isAllAnalysesComplete() && healthVerificationPeriod.equals(HealthVerificationPeriod.POST_ACTIVITY)) {
      this.setStatus(AnalysisStatus.SUCCESS);
    }
    if (analysisCompletedUntil.toEpochMilli() != 0) {
      healthVerificationService.updateProgress(
          getInputs().getVerificationTaskId(), analysisCompletedUntil, getStatus(), false);
    }
    // TODO: When should we mark this as failed/error ?
    // TODO: What to do if the analysis had stopped for some reason for one of the underlying configs.
    return this.getStatus();
  }

  @Override
  public AnalysisState handleRerun() {
    return null;
  }

  @Override
  public AnalysisState handleRunning() {
    // update any new analyses if possible.
    analysisCompletedUntil = healthVerificationService.aggregateActivityAnalysis(getInputs().getVerificationTaskId(),
        getInputs().getStartTime(), getInputs().getEndTime(), analysisCompletedUntil, healthVerificationPeriod);
    return this;
  }

  @Override
  public AnalysisState handleSuccess() {
    this.setStatus(AnalysisStatus.COMPLETED);
    healthVerificationService.updateProgress(
        getInputs().getVerificationTaskId(), analysisCompletedUntil, getStatus(), true);
    return this;
  }

  @Override
  public void handleFinalStatuses(AnalysisStatus finalStatus) {
    healthVerificationService.updateProgress(
        getInputs().getVerificationTaskId(), analysisCompletedUntil, getStatus(), true);
  }

  @Override
  public AnalysisState handleTransition() {
    switch (healthVerificationPeriod) {
      case PRE_ACTIVITY:
        ActivityVerificationState postActivityVerificationState =
            ActivityVerificationState.builder()
                .preActivityVerificationStartTime(preActivityVerificationStartTime)
                .postActivityVerificationStartTime(postActivityVerificationStartTime)
                .healthVerificationPeriod(HealthVerificationPeriod.POST_ACTIVITY)
                .build();
        AnalysisInput input = AnalysisInput.builder()
                                  .verificationTaskId(getInputs().getVerificationTaskId())
                                  .startTime(postActivityVerificationStartTime)
                                  .endTime(postActivityVerificationStartTime.plus(getDurationObj()))
                                  .build();
        postActivityVerificationState.setInputs(input);
        postActivityVerificationState.setStatus(AnalysisStatus.CREATED);
        return postActivityVerificationState;

      case POST_ACTIVITY:
        this.setStatus(AnalysisStatus.SUCCESS);
        return this;
      default:
        throw new AnalysisStateMachineException("Unknown period in health verification");
    }
  }

  @Override
  public AnalysisState handleRetry() {
    return null;
  }
}
