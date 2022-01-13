/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.support.reconciliation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class SupportRoleAssignmentsReconciliationService implements Managed {
  private final SupportRoleAssignmentsReconciliationJob reconciliationJob;
  private final ScheduledExecutorService executorService;
  private Future<?> reconciliationFuture;

  @Inject
  public SupportRoleAssignmentsReconciliationService(SupportRoleAssignmentsReconciliationJob reconciliationJob) {
    this.reconciliationJob = reconciliationJob;
    this.executorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("support-role-assignments-reconciliation-%d").build());
  }

  @Override
  public void start() throws Exception {
    if (reconciliationFuture == null && !executorService.isShutdown()) {
      reconciliationFuture = executorService.scheduleWithFixedDelay(reconciliationJob, 0, 150, TimeUnit.SECONDS);
    }
  }

  @Override
  public void stop() throws Exception {
    if (reconciliationFuture != null) {
      reconciliationFuture.cancel(true);
    }
    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.MINUTES);
  }
}
