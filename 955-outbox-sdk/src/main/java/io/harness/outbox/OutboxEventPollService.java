/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@OwnedBy(PL)
public class OutboxEventPollService implements Managed {
  private final OutboxEventPollJob outboxEventPollJob;
  private final OutboxPollConfiguration outboxPollConfiguration;
  private final ScheduledExecutorService executorService;
  private Future<?> outboxPollJobFuture;

  @Inject
  public OutboxEventPollService(
      OutboxEventPollJob outboxEventPollJob, OutboxPollConfiguration outboxPollConfiguration) {
    this.outboxEventPollJob = outboxEventPollJob;
    this.outboxPollConfiguration = outboxPollConfiguration;
    String threadName = "outbox-poll-service-thread-" + outboxPollConfiguration.getLockId();
    this.executorService =
        Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(threadName).build());
  }

  @Override
  public void start() {
    outboxPollJobFuture =
        executorService.scheduleAtFixedRate(outboxEventPollJob, outboxPollConfiguration.getInitialDelayInSeconds(),
            outboxPollConfiguration.getPollingIntervalInSeconds(), TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    outboxPollJobFuture.cancel(false);
    executorService.shutdownNow();
  }
}
