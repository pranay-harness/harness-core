package io.harness.outbox;

import io.harness.outbox.api.OutboxEventHandler;
import io.harness.outbox.api.OutboxEventService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OutboxEventPollJob implements Runnable {
  private final OutboxEventService outboxEventService;
  private final OutboxEventHandler outboxEventHandler;

  @Inject
  public OutboxEventPollJob(OutboxEventService outboxEventService, OutboxEventHandler outboxEventHandler) {
    this.outboxEventService = outboxEventService;
    this.outboxEventHandler = outboxEventHandler;
  }

  @Override
  public void run() {
    List<OutboxEvent> outboxEvents = outboxEventService.list(null).getContent();
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
