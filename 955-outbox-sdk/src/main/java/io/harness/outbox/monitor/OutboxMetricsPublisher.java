/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.outbox.monitor;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.outbox.OutboxSDKConstants.ALL_EVENT_TYPES;
import static io.harness.outbox.OutboxSDKConstants.OUTBOX_BLOCKED_QUEUE_SIZE_METRIC_NAME;
import static io.harness.outbox.OutboxSDKConstants.OUTBOX_QUEUE_SIZE_METRIC_NAME;
import static io.harness.outbox.TransactionOutboxModule.SERVICE_ID_FOR_OUTBOX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.metrics.service.api.MetricService;
import io.harness.metrics.service.api.MetricsPublisher;
import io.harness.outbox.api.OutboxDao;
import io.harness.outbox.filter.OutboxMetricsFilter;
import io.harness.outbox.monitor.context.OutboxContext;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Map;

@OwnedBy(PL)
public class OutboxMetricsPublisher implements MetricsPublisher {
  private static final OutboxMetricsFilter BLOCKED_QUEUE_SIZE_FILTER =
      OutboxMetricsFilter.builder().blocked(true).build();
  private static final OutboxMetricsFilter QUEUE_SIZE_FILTER = OutboxMetricsFilter.builder().build();
  private final OutboxDao outboxDao;
  private final MetricService metricService;
  private final String serviceId;

  @Inject
  public OutboxMetricsPublisher(
      OutboxDao outboxDao, MetricService metricService, @Named(SERVICE_ID_FOR_OUTBOX) String serviceId) {
    this.outboxDao = outboxDao;
    this.metricService = metricService;
    this.serviceId = serviceId;
  }

  @Override
  public void recordMetrics() {
    try (OutboxContext ignored = new OutboxContext(serviceId, ALL_EVENT_TYPES)) {
      metricService.recordMetric(OUTBOX_QUEUE_SIZE_METRIC_NAME, outboxDao.count(QUEUE_SIZE_FILTER));
      metricService.recordMetric(OUTBOX_BLOCKED_QUEUE_SIZE_METRIC_NAME, outboxDao.count(BLOCKED_QUEUE_SIZE_FILTER));
    }

    Map<String, Long> countPerEventType = outboxDao.countPerEventType(QUEUE_SIZE_FILTER);
    countPerEventType.forEach((eventType, count) -> {
      try (OutboxContext ignored = new OutboxContext(serviceId, eventType)) {
        metricService.recordMetric(OUTBOX_QUEUE_SIZE_METRIC_NAME, count);
      }
    });

    Map<String, Long> blockedCountPerEventType = outboxDao.countPerEventType(BLOCKED_QUEUE_SIZE_FILTER);
    blockedCountPerEventType.forEach((eventType, count) -> {
      try (OutboxContext ignored = new OutboxContext(serviceId, eventType)) {
        metricService.recordMetric(OUTBOX_BLOCKED_QUEUE_SIZE_METRIC_NAME, count);
      }
    });
  }
}
