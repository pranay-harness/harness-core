package io.harness.outbox;

import static io.harness.outbox.OutboxSDKConstants.DEFAULT_OUTBOX_POLL_PAGE_REQUEST;

import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.ng.beans.PageRequest;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.outbox.api.OutboxEventService;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OutboxEventPollJob implements Runnable {
  private final OutboxEventService outboxEventService;
  private final OutboxEventHandler outboxEventHandler;
  private final PersistentLocker persistentLocker;
  private final PageRequest pageRequest;
  private static final String OUTBOX_POLL_JOB_LOCK = "OUTBOX_POLL_JOB_LOCK";

  @Inject
  public OutboxEventPollJob(
      OutboxEventService outboxEventService, OutboxEventHandler outboxEventHandler, PersistentLocker persistentLocker) {
    this.outboxEventService = outboxEventService;
    this.outboxEventHandler = outboxEventHandler;
    this.persistentLocker = persistentLocker;
    this.pageRequest = DEFAULT_OUTBOX_POLL_PAGE_REQUEST;
  }

  @Override
  public void run() {
    try (AcquiredLock<?> lock = persistentLocker.tryToAcquireLock(OUTBOX_POLL_JOB_LOCK, Duration.ofMinutes(1))) {
      if (lock == null) {
        log.error("Could not acquire lock for outbox poll job");
        return;
      }
      List<OutboxEvent> outboxEvents = outboxEventService.list(pageRequest).getContent();
      outboxEvents.forEach(outbox -> {
        try {
          if (outboxEventHandler.handle(outbox)) {
            outboxEventService.delete(outbox.getId());
          }
        } catch (Exception exception) {
          log.error(String.format("Error occurred while handling outbox event with id %s and type %s", outbox.getId(),
                        outbox.getEventType()),
              exception);
        }
      });
    }
  }
}
