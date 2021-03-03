package io.harness.outbox.api;

import io.harness.outbox.Outbox;

import java.util.List;

public interface OutboxService {
  Outbox save(Outbox outbox);

  List<Outbox> list();

  boolean delete(String uuid);
}
