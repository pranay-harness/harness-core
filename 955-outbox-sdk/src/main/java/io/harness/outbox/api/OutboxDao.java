package io.harness.outbox.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.outbox.OutboxEvent;

@OwnedBy(PL)
public interface OutboxDao {
  OutboxEvent save(OutboxEvent outboxEvent);

  PageResponse<OutboxEvent> list(PageRequest pageRequest);

  boolean delete(String outboxEventId);
}
