/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.metrics.impl;

import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Group.id;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.metrics.beans.DelegateMetricContext;
import io.harness.metrics.service.api.MetricService;
import io.harness.metrics.service.api.MetricsPublisher;
import io.harness.persistence.HPersistence;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;

@Slf4j
public class DelegateTaskMetricsPublisher implements MetricsPublisher {
  @Inject private MetricService metricService;
  @Inject private HPersistence persistence;
  private static final String ACTIVE_DELEGATES_COUNT = "active_delegates_count";

  @Override
  public void recordMetrics() {
    sendTaskStatusMetrics();
  }

  @VisibleForTesting
  void sendTaskStatusMetrics() {
    log.info("Starting getting delegate metrics.");
    long startTime = Instant.now().toEpochMilli();

    Query<Delegate> query = persistence.createQuery(Delegate.class)
                                .field(DelegateKeys.status)
                                .equal(DelegateInstanceStatus.ENABLED)
                                .field(DelegateKeys.lastHeartBeat)
                                .greaterThan(Instant.now().minusSeconds(60).toEpochMilli());
    persistence.getDatastore(Delegate.class)
        .createAggregation(Delegate.class)
        .match(query)
        .group(id(grouping("accountId", "accountId")), grouping("count", accumulator("$sum", 1)))
        .aggregate(DelegateAggregator.class)
        .forEachRemaining(instanceCount -> {
          try (DelegateMetricContext ignore = new DelegateMetricContext(instanceCount.id.accountId)) {
            metricService.recordMetric(ACTIVE_DELEGATES_COUNT, instanceCount.count);
          }
        });

    log.info("Total time taken to collect metrics for class {} {} (ms)", DelegateTask.class.getSimpleName(),
        Instant.now().toEpochMilli() - startTime);
  }

  @Data
  @NoArgsConstructor
  private static class DelegateAggregator {
    @Id ID id;
    int count;
  }
  @Data
  @NoArgsConstructor
  private static class ID {
    String accountId;
  }
}
