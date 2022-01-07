/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc;

import io.harness.beans.DelegateTask;
import io.harness.delegate.TaskExecutionStage;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DelegateTaskGrpcUtils {
  public static TaskExecutionStage mapTaskStatusToTaskExecutionStage(DelegateTask.Status taskStatus) {
    switch (taskStatus) {
      case PARKED:
        return TaskExecutionStage.PARKED;
      case QUEUED:
        return TaskExecutionStage.QUEUEING;
      case STARTED:
        return TaskExecutionStage.EXECUTING;
      case ERROR:
        return TaskExecutionStage.FAILED;
      case ABORTED:
        return TaskExecutionStage.ABORTED;
      default:
        return TaskExecutionStage.TYPE_UNSPECIFIED;
    }
  }
}
