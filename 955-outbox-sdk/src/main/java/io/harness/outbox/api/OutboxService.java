package io.harness.outbox.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.filter.OutboxEventFilter;

import java.util.List;

@OwnedBy(PL)
public interface OutboxService {
  OutboxEvent save(Event event);

  OutboxEvent update(OutboxEvent outboxEvent);

  List<OutboxEvent> list(OutboxEventFilter outboxEventFilter);

  boolean delete(String outboxEventId);
}
