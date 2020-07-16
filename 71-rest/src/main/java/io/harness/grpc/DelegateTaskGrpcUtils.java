package io.harness.grpc;

import io.harness.beans.DelegateTask;
import io.harness.delegate.NgTaskExecutionStage;
import io.harness.delegate.TaskExecutionStage;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DelegateTaskGrpcUtils {
  public static TaskExecutionStage mapTaskStatusToTaskExecutionStage(DelegateTask.Status taskStatus) {
    switch (taskStatus) {
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

  public static NgTaskExecutionStage mapTaskStatusToNgTaskExecutionStage(DelegateTask.Status taskStatus) {
    switch (taskStatus) {
      case QUEUED:
        return NgTaskExecutionStage.QUEUEING;
      case STARTED:
        return NgTaskExecutionStage.EXECUTING;
      case ERROR:
        return NgTaskExecutionStage.FAILED;
      case ABORTED:
        return NgTaskExecutionStage.ABORTED;
      default:
        return NgTaskExecutionStage.TYPE_UNSPECIFIED;
    }
  }
}
