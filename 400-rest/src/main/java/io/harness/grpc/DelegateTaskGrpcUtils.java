package io.harness.grpc;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DelegateTask;
import io.harness.delegate.TaskExecutionStage;

import lombok.experimental.UtilityClass;

@UtilityClass
@TargetModule(Module._420_DELEGATE_SERVICE)
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
