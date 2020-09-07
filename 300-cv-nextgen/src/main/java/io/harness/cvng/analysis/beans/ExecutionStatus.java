package io.harness.cvng.analysis.beans;

import com.google.common.collect.Lists;

import java.util.List;

public enum ExecutionStatus {
  QUEUED,
  RUNNING,
  FAILED,
  SUCCESS,
  TIMEOUT;

  public static List<ExecutionStatus> finalStatuses() {
    return Lists.newArrayList(SUCCESS, FAILED, TIMEOUT);
  }
}
