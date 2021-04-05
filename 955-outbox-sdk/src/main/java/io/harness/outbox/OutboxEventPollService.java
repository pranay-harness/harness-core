package io.harness.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_OUTBOX_POLL_CONFIGURATION;

import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

@OwnedBy(PL)
public class OutboxEventPollService implements Managed {
  private final OutboxEventPollJob outboxEventPollJob;
  private final OutboxPollConfiguration outboxPollConfiguration;
  private final ScheduledExecutorService executorService;
  private Future<?> outboxPollJobFuture;

  @Inject
  public OutboxEventPollService(
      OutboxEventPollJob outboxEventPollJob, @Nullable OutboxPollConfiguration outboxPollConfiguration) {
    this.outboxEventPollJob = outboxEventPollJob;
    this.outboxPollConfiguration =
        outboxPollConfiguration == null ? DEFAULT_OUTBOX_POLL_CONFIGURATION : outboxPollConfiguration;
    this.executorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("outbox-poll-service-thread").build());
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
