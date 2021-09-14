/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.outbox.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.filter.OutboxEventFilter;
import io.harness.outbox.filter.OutboxMetricsFilter;

import java.util.List;
import java.util.Map;

@OwnedBy(PL)
public interface OutboxDao {
  OutboxEvent save(OutboxEvent outboxEvent);

  List<OutboxEvent> list(OutboxEventFilter outboxEventFilter);

  long count(OutboxMetricsFilter outboxMetricsFilter);

  Map<String, Long> countPerEventType(OutboxMetricsFilter outboxMetricsFilter);

  boolean delete(String outboxEventId);
}
