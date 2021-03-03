package io.harness.outbox;

import io.harness.outbox.api.OutboxHandler;
import io.harness.outbox.api.OutboxService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OutboxPollJob implements Runnable {
  private final OutboxService outboxService;
  private final OutboxHandler outboxHandler;

  @Inject
  public OutboxPollJob(OutboxService outboxService, OutboxHandler outboxHandler) {
    this.outboxService = outboxService;
    this.outboxHandler = outboxHandler;
  }

  @Override
  public void run() {
    List<Outbox> outboxes = outboxService.list();
    outboxes.forEach(outbox -> {
      try {
        if (outboxHandler.handle(outbox)) {
          outboxService.delete(outbox.getId());
        }
      } catch (Exception exception) {
        log.error(String.format("Error occurred while handling outbox event with id %s and type %s", outbox.getId(),
                      outbox.getType()),
            exception);
      }
    });
  }
}
