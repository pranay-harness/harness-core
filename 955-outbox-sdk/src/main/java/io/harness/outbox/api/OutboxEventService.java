package io.harness.outbox.api;

import io.harness.HEvent;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.outbox.OutboxEvent;

public interface OutboxEventService {
  OutboxEvent save(HEvent event);

  PageResponse<OutboxEvent> list(PageRequest pageRequest);

  boolean delete(String outboxEventId);
}
