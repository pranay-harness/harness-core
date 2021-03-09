package io.harness.outbox.api;

import io.harness.Event;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.outbox.OutboxEvent;

public interface OutboxService {
  OutboxEvent save(Event event);

  PageResponse<OutboxEvent> list(PageRequest pageRequest);

  boolean delete(String outboxEventId);
}
