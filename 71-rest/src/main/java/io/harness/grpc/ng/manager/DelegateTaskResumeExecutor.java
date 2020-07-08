package io.harness.grpc.ng.manager;

import io.harness.NgManagerServiceDriver;
import io.harness.delegate.beans.ResponseData;
import lombok.Builder;

public class DelegateTaskResumeExecutor implements Runnable {
  String taskId;
  ResponseData responseData;
  NgManagerServiceDriver ngManagerServiceDriver;

  @Builder
  public DelegateTaskResumeExecutor(
      String taskId, ResponseData responseData, NgManagerServiceDriver ngManagerServiceDriver) {
    this.taskId = taskId;
    this.responseData = responseData;
    this.ngManagerServiceDriver = ngManagerServiceDriver;
  }

  @Override
  public void run() {
    ngManagerServiceDriver.sendTaskResult(taskId, responseData);
  }
}
