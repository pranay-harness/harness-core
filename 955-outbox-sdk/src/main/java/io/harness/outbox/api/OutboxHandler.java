package io.harness.outbox.api;

import io.harness.outbox.Outbox;

public interface OutboxHandler {
  boolean handle(Outbox outbox);
}
