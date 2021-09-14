/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.beans;

import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ExecutionStatus;

import java.util.Arrays;
import java.util.List;

public enum AnalysisStatus {
  CREATED,
  RUNNING,
  SUCCESS,
  RETRY,
  TRANSITION,
  IGNORED,
  TIMEOUT,
  FAILED,
  COMPLETED;

  public static ExecutionStatus mapToVerificationJobExecutionStatus(AnalysisStatus analysisStatus) {
    switch (analysisStatus) {
      case SUCCESS:
        return ExecutionStatus.SUCCESS;
      case FAILED:
        return ExecutionStatus.FAILED;
      case TIMEOUT:
        return ExecutionStatus.TIMEOUT;
      default:
        throw new IllegalStateException("AnalysisStatus " + analysisStatus
            + " should be one of final status. Mapping to executionStatus not defined.");
    }
  }

  public static List<AnalysisStatus> getFinalStates() {
    return Arrays.asList(COMPLETED, FAILED, TIMEOUT);
  }
  public static List<AnalysisStatus> getFailedStatuses() {
    return Arrays.asList(FAILED, TIMEOUT);
  }
  public static List<AnalysisStatus> getNonFinalStatuses() {
    return Arrays.asList(CREATED, RUNNING, RETRY, TRANSITION);
  }
}
