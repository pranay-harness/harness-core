package io.harness.audit.retention;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import io.dropwizard.lifecycle.Managed;
import io.harness.annotations.dev.OwnedBy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@OwnedBy(PL)
@Slf4j
public class AuditAccountSyncService implements Managed {
  @Inject AuditAccountSyncJob auditAccountSyncJob;
  private Future<?> auditAccountSyncJobFuture;
  final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder().setNameFormat("audit-account-sync-thread").build());

  @Override
  public void start()  {
    auditAccountSyncJobFuture = executorService.scheduleAtFixedRate(auditAccountSyncJob, 1, 6, TimeUnit.HOURS);
  }

  @Override
  public void stop()  {
    auditAccountSyncJobFuture.cancel(true);
    executorService.shutdownNow();
  }
}
