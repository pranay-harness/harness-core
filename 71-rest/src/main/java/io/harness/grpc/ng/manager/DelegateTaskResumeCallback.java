package io.harness.grpc.ng.manager;

import com.google.inject.Inject;

import io.harness.NgManagerServiceDriver;
import io.harness.delegate.beans.ResponseData;
import io.harness.waiter.NotifyCallback;
import lombok.Builder;

import java.util.Map;
import java.util.concurrent.ExecutorService;

public class DelegateTaskResumeCallback implements NotifyCallback {
  @Inject private NgManagerServiceDriver ngManagerServiceDriver;
  @Inject private ExecutorService executorService;

  String taskId;

  @Builder
  public DelegateTaskResumeCallback(String taskId) {
    this.taskId = taskId;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    notifyDriver(response);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    notifyDriver(response);
  }

  private void notifyDriver(Map<String, ResponseData> response) {
    DelegateTaskResumeExecutor resumeExecutor = DelegateTaskResumeExecutor.builder()
                                                    .taskId(taskId)
                                                    .responseData(response.values().iterator().next())
                                                    .ngManagerServiceDriver(ngManagerServiceDriver)
                                                    .build();
    executorService.submit(resumeExecutor);
  }
}
