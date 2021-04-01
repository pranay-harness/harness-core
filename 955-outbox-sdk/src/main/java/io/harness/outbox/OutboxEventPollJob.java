package io.harness.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_MAX_ATTEMPTS;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_OUTBOX_POLL_PAGE_REQUEST;

import io.harness.annotations.dev.OwnedBy;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.ng.beans.PageRequest;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.outbox.api.OutboxService;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class OutboxEventPollJob implements Runnable {
  private final OutboxService outboxService;
  private final OutboxEventHandler outboxEventHandler;
  private final PersistentLocker persistentLocker;
  private final PageRequest pageRequest;
  private final long maxPollingAttempts;
  private static final String OUTBOX_POLL_JOB_LOCK = "OUTBOX_POLL_JOB_LOCK";

  @Inject
  public OutboxEventPollJob(
      OutboxService outboxService, OutboxEventHandler outboxEventHandler, PersistentLocker persistentLocker) {
    this(outboxService, outboxEventHandler, persistentLocker, DEFAULT_OUTBOX_POLL_PAGE_REQUEST, DEFAULT_MAX_ATTEMPTS);
  }

  public OutboxEventPollJob(OutboxService outboxService, OutboxEventHandler outboxEventHandler,
      PersistentLocker persistentLocker, PageRequest pageRequest, long maxPollingAttempts) {
    this.outboxService = outboxService;
    this.outboxEventHandler = outboxEventHandler;
    this.persistentLocker = persistentLocker;
    this.pageRequest = pageRequest;
    this.maxPollingAttempts = maxPollingAttempts;
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
      List<OutboxEvent> outboxEvents = outboxService.list(pageRequest).getContent();
      for (OutboxEvent outbox : outboxEvents) {
        boolean success = false;
        try {
          success = outboxEventHandler.handle(outbox);
        } catch (Exception exception) {
          log.error(String.format("Error occurred while handling outbox event with id %s and type %s", outbox.getId(),
                        outbox.getEventType()),
              exception);
        }
        try {
          if (success) {
            outboxService.delete(outbox.getId());
          } else {
            long timesPolled = outbox.getAttempts() == null ? 0 : outbox.getAttempts();
            if (timesPolled + 1 >= maxPollingAttempts) {
              outbox.setBlocked(true);
            }
            outbox.setAttempts(timesPolled + 1);
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
}
