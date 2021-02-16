package io.harness;

import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChangeDataCaptureJob implements Runnable {
  @Inject private Provider<ChangeDataCaptureTask> changeDataCaptureTaskProvider;
  private ChangeDataCaptureTask changeDataCaptureTask;
  @Override
  public void run() {
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
