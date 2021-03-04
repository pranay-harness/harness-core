package io.harness.outbox.api;

import io.harness.ng.beans.PageResponse;
import io.harness.outbox.Outbox;
import io.harness.outbox.OutboxFilter;

public interface OutboxService {
  Outbox save(Outbox outbox);

  PageResponse<Outbox> list(OutboxFilter outboxFilter);

  boolean delete(String outboxId);
}
