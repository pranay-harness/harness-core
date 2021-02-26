package io.harness.testing;

import io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DelegateTaskStressTestThread extends Thread {
  DelegateServiceBlockingStub delegateServiceBlockingStub;
  DelegateTaskStressTestStage stage;
  String stageId;

  public DelegateTaskStressTestThread(
      DelegateServiceBlockingStub delegateServiceBlockingStub, DelegateTaskStressTestStage stage, String stageId) {
    this.delegateServiceBlockingStub = delegateServiceBlockingStub;
    this.stage = stage;
    this.stageId = stageId;
  }

  @Override
  public void run() {
    try {
      Thread.sleep(stage.getOffset() * 1000);
      int taskRequestCount = stage.getTaskRequestCount();

      for (int i = 0; i < stage.getIterations(); i++) {
        int item = (int) (Math.random() * taskRequestCount);
        log.info("Firing iteration " + i + "on stage " + stageId);
        delegateServiceBlockingStub.submitTask(stage.getTaskRequest(item));
        Thread.sleep(1000 / stage.getQps());
      }
    } catch (Exception e) {
      log.warn("Caught exception: " + e);
    }
  }
}
