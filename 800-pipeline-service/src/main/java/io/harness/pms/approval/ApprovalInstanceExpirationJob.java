/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.approval;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.approval.step.ApprovalInstanceService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Singleton
@Slf4j
public class ApprovalInstanceExpirationJob implements Managed {
  @Inject private ApprovalInstanceService approvalInstanceService;

  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder().setNameFormat("approval-expiration-job").build());
  private Future<?> jobFuture;

  @Override
  public void start() {
    jobFuture = executorService.scheduleAtFixedRate(this::run, 5, 10, TimeUnit.MINUTES);
  }

  @Override
  public void stop() throws Exception {
    if (jobFuture != null) {
      jobFuture.cancel(true);
    }
    executorService.shutdown();
    executorService.awaitTermination(5, TimeUnit.SECONDS);
  }

  private void run() {
    log.info("Running ApprovalInstanceExpirationJob");
    approvalInstanceService.markExpiredInstances();
  }
}
