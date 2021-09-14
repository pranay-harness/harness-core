/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
