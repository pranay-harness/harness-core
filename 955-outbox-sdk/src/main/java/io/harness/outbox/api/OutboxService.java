package io.harness.outbox.api;

import io.harness.outbox.Outbox;
import io.harness.outbox.OutboxFilter;

import java.util.List;

public interface OutboxService {
  Outbox save(Outbox outbox);

  List<Outbox> list(OutboxFilter outboxFilter);

  boolean delete(String outboxId);
}
