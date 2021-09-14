/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.stresstesting.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;
import io.harness.testing.DelegateTaskStressTestStage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
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
      int taskRequestCount = stage.getTaskRequestCount();

      for (int i = 0; i < stage.getIterations(); i++) {
        int item = (int) (Math.random() * taskRequestCount);
        String taskId = delegateServiceBlockingStub.submitTask(stage.getTaskRequest(item)).getTaskId().getId();
        log.info("Firing iteration " + i + "on stage " + stageId + "task id " + taskId);
        Thread.sleep(1000 / stage.getQps());
      }
    } catch (Exception e) {
      log.warn("Caught exception: " + e);
    }
  }
}
