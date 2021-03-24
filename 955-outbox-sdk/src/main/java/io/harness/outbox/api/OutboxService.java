package io.harness.outbox.api;

import io.harness.event.Event;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.outbox.OutboxEvent;

public interface OutboxService {
  OutboxEvent save(Event event);

  OutboxEvent update(OutboxEvent outboxEvent);

  PageResponse<OutboxEvent> list(PageRequest pageRequest);

  boolean delete(String outboxEventId);
}
