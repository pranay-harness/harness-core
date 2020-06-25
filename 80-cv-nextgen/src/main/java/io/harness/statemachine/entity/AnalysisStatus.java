package io.harness.statemachine.entity;

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

  public static List<AnalysisStatus> getFinalStates() {
    return Arrays.asList(COMPLETED, FAILED, TIMEOUT);
  }
}
