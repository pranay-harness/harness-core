package io.harness.outbox.api;

import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.outbox.OutboxEvent;

public interface OutboxEventService {
  OutboxEvent save(OutboxEvent outboxEvent);

  PageResponse<OutboxEvent> list(PageRequest pageRequest);

  boolean delete(String outboxId);
}
