package io.harness.resourcegroup.reconciliation;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResourceGroupSyncConciliationService implements Managed {
  @Inject SyncConciliationMasterJob syncConciliationMasterJob;
  final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder().setNameFormat("sync-conciliation-main-thread").build());
  Future syncConciliationJobFuture;

  @Override
  public void start() throws Exception {
    syncConciliationJobFuture = executorService.scheduleAtFixedRate(syncConciliationMasterJob, 5, 5, TimeUnit.SECONDS);
  }

  @Override
  public void stop() throws Exception {
    syncConciliationJobFuture.cancel(true);
    executorService.shutdownNow();
  }
}
