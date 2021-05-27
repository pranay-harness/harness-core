package io.harness.cvng.beans;

import java.util.Arrays;
import java.util.List;

public enum DataCollectionExecutionStatus {
  FAILED,
  QUEUED,
  RUNNING,
  WAITING,
  EXPIRED,
  SUCCESS;
  public static List<DataCollectionExecutionStatus> getFailedStatuses() {
    return Arrays.asList(FAILED, EXPIRED);
  }
  public static List<DataCollectionExecutionStatus> getNonFinalStatues() {
    return Arrays.asList(QUEUED, RUNNING, WAITING);
  }
}
