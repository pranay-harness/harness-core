package io.harness.resourcegroup.reconciliation;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class SyncConciliationMasterJob implements Runnable {
  @Inject SyncConciliationJob syncConciliationJob;
  final ExecutorService executorService = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("sync-conciliation-worker-thread").build());
  Future syncConciliationJobFuture;

  @Override
  public void run() {
    if (Thread.currentThread().isInterrupted()) {
      syncConciliationJobFuture.cancel(true);
      log.info("{} thread got interruted", this.getClass().getName());
      Thread.currentThread().interrupt();
    } else if (syncConciliationJobFuture == null || syncConciliationJobFuture.isCancelled()) {
      syncConciliationJobFuture = executorService.submit(syncConciliationJob);
    }
  }
}
