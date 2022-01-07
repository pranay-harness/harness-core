/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
