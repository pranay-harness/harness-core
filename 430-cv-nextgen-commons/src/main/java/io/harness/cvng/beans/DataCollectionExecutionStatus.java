/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import java.util.Arrays;
import java.util.List;

public enum DataCollectionExecutionStatus {
  FAILED,
  QUEUED,
  RUNNING,
  WAITING,
  EXPIRED,
  SUCCESS,
  ABORTED;
  public static List<DataCollectionExecutionStatus> getFailedStatuses() {
    return Arrays.asList(FAILED, EXPIRED);
  }
  public static List<DataCollectionExecutionStatus> getNonFinalStatues() {
    return Arrays.asList(QUEUED, RUNNING, WAITING);
  }
}
