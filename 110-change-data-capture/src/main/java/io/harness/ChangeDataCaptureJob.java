/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CE)
public class ChangeDataCaptureJob implements Runnable {
  @Inject private Provider<ChangeDataCaptureBulkSyncTask> changeDataCaptureBulkSyncTaskProvider;
  @Inject private Provider<ChangeDataCaptureTask> changeDataCaptureTaskProvider;
  private ChangeDataCaptureTask changeDataCaptureTask;
  private ChangeDataCaptureBulkSyncTask changeDataCaptureBulkSyncTask;
  @Override
  public void run() {
    changeDataCaptureBulkSyncTask = changeDataCaptureBulkSyncTaskProvider.get();
    changeDataCaptureBulkSyncTask.run();

    changeDataCaptureTask = changeDataCaptureTaskProvider.get();

    try {
      changeDataCaptureTask.run();
    } catch (RuntimeException e) {
      log.error("CDC Sync Job unexpectedly stopped", e);
    } catch (InterruptedException e) {
      log.error("CDC Sync job interrupted", e);
      Thread.currentThread().interrupt();
    } finally {
      log.info("CDC sync job has stopped");
      stop();
    }
  }

  private void stop() {
    changeDataCaptureTask.stop();
  }
}
