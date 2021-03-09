package io.harness.outbox;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OutboxEventPollService implements Managed {
  private final OutboxEventPollJob outboxEventPollJob;
  private final ScheduledExecutorService executorService;
  private Future<?> outboxPollJobFuture;

  @Inject
  public OutboxEventPollService(OutboxEventPollJob outboxEventPollJob) {
    this.outboxEventPollJob = outboxEventPollJob;
    this.executorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("outbox-poll-service-thread").build());
  }

  @Override
  public void start() {
    outboxPollJobFuture = executorService.scheduleWithFixedDelay(outboxEventPollJob, 20, 10, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    outboxPollJobFuture.cancel(false);
    executorService.shutdownNow();
  }
}
