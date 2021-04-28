package io.harness.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_MAX_EVENTS_POLLED;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_OUTBOX_POLL_CONFIGURATION;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_UNBLOCK_RETRY_INTERVAL_IN_MINUTES;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.outbox.api.OutboxService;
import io.harness.outbox.filter.OutboxEventFilter;

import com.google.inject.Inject;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class OutboxEventPollJob implements Runnable {
  private final OutboxService outboxService;
  private final OutboxEventHandler outboxEventHandler;
  private final PersistentLocker persistentLocker;
  private final OutboxPollConfiguration outboxPollConfiguration;
  private final OutboxEventFilter outboxEventFilter;
  private final Retry retry;
  private static final String OUTBOX_POLL_JOB_LOCK = "OUTBOX_POLL_JOB_LOCK";

  @Inject
  public OutboxEventPollJob(OutboxService outboxService, OutboxEventHandler outboxEventHandler,
      PersistentLocker persistentLocker, @Nullable OutboxPollConfiguration outboxPollConfiguration) {
    this.outboxService = outboxService;
    this.outboxEventHandler = outboxEventHandler;
    this.persistentLocker = persistentLocker;
    this.outboxPollConfiguration =
        outboxPollConfiguration == null ? DEFAULT_OUTBOX_POLL_CONFIGURATION : outboxPollConfiguration;
    this.outboxEventFilter = OutboxEventFilter.builder().maximumEventsPolled(DEFAULT_MAX_EVENTS_POLLED).build();
    RetryConfig retryConfig = RetryConfig.custom()
                                  .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 1.5))
                                  .maxAttempts(this.outboxPollConfiguration.getMaximumRetryAttemptsForAnEvent())
                                  .build();
    this.retry = Retry.of("outboxEventHandleRetry", retryConfig);
  }

  @Override
  public void run() {
    try {
      pollAndHandleOutboxEvents();
    } catch (Exception exception) {
      log.error("Unexpected error occurred during the execution of OutboxPollJob", exception);
    }
  }

  private void pollAndHandleOutboxEvents() {
    try (AcquiredLock<?> lock = persistentLocker.tryToAcquireLock(OUTBOX_POLL_JOB_LOCK, Duration.ofMinutes(2))) {
      if (lock == null) {
        log.error("Could not acquire lock for outbox poll job");
        return;
      }
      List<OutboxEvent> outboxEvents;
      try {
        outboxEvents = outboxService.list(outboxEventFilter);
      } catch (InstantiationError error) {
        log.error("InstantiationError occurred while fetching entries from the outbox", error);
        return;
      }

      for (OutboxEvent outbox : outboxEvents) {
        long startTime = System.currentTimeMillis();
        boolean success = handle(outbox);
        log.info(String.format("Took %d milliseconds for outbox event handling for id %s and eventType %s.",
            System.currentTimeMillis() - startTime, outbox.getId(), outbox.getEventType()));
        try {
          if (success) {
            outboxService.delete(outbox.getId());
          } else {
            outbox.setBlocked(true);
            outbox.setNextUnblockAttemptAt(
                Instant.now().plus(DEFAULT_UNBLOCK_RETRY_INTERVAL_IN_MINUTES, ChronoUnit.MINUTES));
            outboxService.update(outbox);
          }
        } catch (Exception exception) {
          log.error(String.format("Error occurred in post handling of outbox event with id %s and type %s",
                        outbox.getId(), outbox.getEventType()),
              exception);
        }
      }
    }
  }

  private boolean handle(OutboxEvent outboxEvent) {
    boolean success = false;
    try {
      success = outboxEventHandler.handle(outboxEvent);
    } catch (Exception exception) {
      log.error(String.format("Error occurred while handling outbox event with id %s and type %s", outboxEvent.getId(),
                    outboxEvent.getEventType()),
          exception);
    }
    if (!success && !Boolean.TRUE.equals(outboxEvent.getBlocked())) {
      log.error("Retrying this outbox event with exponential backoff now...");
      success = handleWithExponentialBackOff(outboxEvent);
    }
    return success;
  }

  private boolean handleWithExponentialBackOff(OutboxEvent outboxEvent) {
    try {
      return retry.executeSupplier(() -> {
        if (!outboxEventHandler.handle(outboxEvent)) {
          throw new UnexpectedException(String.format(
              "Outbox event handling failed in another retry attempt for event with id %s", outboxEvent.getId()));
        }
        return true;
      });
    } catch (Exception exception) {
      log.error(String.format("Error occurred while handling outbox event with id %s and type %s", outboxEvent.getId(),
                    outboxEvent.getEventType()),
          exception);
      return false;
    }
  }
}
