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
//    changeDataCaptureBulkSyncTask = changeDataCaptureBulkSyncTaskProvider.get();
//    changeDataCaptureBulkSyncTask.run();

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
