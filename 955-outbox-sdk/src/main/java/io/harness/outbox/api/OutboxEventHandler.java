package io.harness.outbox.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.outbox.OutboxEvent;

@OwnedBy(PL)
public interface OutboxEventHandler {
  boolean handle(OutboxEvent outboxEvent);
}
