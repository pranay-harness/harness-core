/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.changestreamsframework.ChangeSubscriber;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CE)
public class ChangeDataCaptureTask {
  @Inject private ChangeDataCaptureHelper changeDataCaptureHelper;
  @Inject private ChangeEventProcessor changeEventProcessor;

  private ChangeSubscriber<?> getChangeSubscriber() {
    return changeEvent -> {
      boolean isRunningSuccessfully = changeEventProcessor.processChangeEvent(changeEvent);
      if (!isRunningSuccessfully) {
        stop();
      }
    };
  }

  public boolean run() throws InterruptedException {
    log.info("Initializing change listeners for CDC entities");
    changeDataCaptureHelper.startChangeListeners(getChangeSubscriber());
    changeEventProcessor.startProcessingChangeEvents();
    boolean isAlive = true;
    while (!Thread.currentThread().isInterrupted() && isAlive) {
      Thread.sleep(2000);
      isAlive = changeDataCaptureHelper.checkIfAnyChangeListenerIsAlive();
      isAlive = isAlive && changeEventProcessor.isAlive();
    }
    return false;
  }

  public void stop() {
    changeDataCaptureHelper.stopChangeListeners();
    changeEventProcessor.shutdown();
  }
}
