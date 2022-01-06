/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.eventsframework.monitor.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.monitor.dto.AggregateRedisStreamMetricsDTO;
import io.harness.eventsframework.monitor.dto.RedisStreamConsumerGroupMetricsDTO;
import io.harness.eventsframework.monitor.dto.RedisStreamMetricsDTO;

import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import service.RedisStreamsMetricsAggregator;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class EventsFrameworkMonitoringRunner implements Runnable {
  private final RedisStreamsMetricsAggregator redisStreamsMetricsAggregator;

  @Inject
  public EventsFrameworkMonitoringRunner(RedisStreamsMetricsAggregator redisStreamsMetricsAggregator) {
    this.redisStreamsMetricsAggregator = redisStreamsMetricsAggregator;
  }

  @Override
  public void run() {
    try {
      while (!Thread.currentThread().isInterrupted()) {
        AggregateRedisStreamMetricsDTO aggregateRedisStreamMetricsDTO = redisStreamsMetricsAggregator.getStreamStats();
        for (RedisStreamMetricsDTO streamMetrics : aggregateRedisStreamMetricsDTO.getRedisStreamMetricsDTOList()) {
          String streamName = streamMetrics.getRedisStreamDTO().getStreamName();
          log.info("\n----------------- {} -----------------", streamName);
          printStreamStat(streamName, "Stream size", streamMetrics.getStreamSize());
          printStreamStat(streamName, "Dead letter queue size", streamMetrics.getDeadLetterQueueSize());
          printStreamStat(streamName, "Memory usage of the stream (in MB)", streamMetrics.getMemoryUsageInMBs());
          printStreamStat(streamName, "Average message size (in KB)", streamMetrics.getAverageMessageSizeInKBs());
          log.info("{} - Consumer group stats ----", streamName);
          for (RedisStreamConsumerGroupMetricsDTO consumerGroupMetricsDTO :
              streamMetrics.getConsumergroupMetricsDTOs()) {
            String consumergroupName = consumerGroupMetricsDTO.getConsumergroupName();
            printConsumerGroupStat(
                streamName, consumergroupName, "Pending count", consumerGroupMetricsDTO.getPendingCount());
            printConsumerGroupStat(
                streamName, consumergroupName, "Behind by count", consumerGroupMetricsDTO.getBehindByCount());
          }
        }
        TimeUnit.SECONDS.sleep(60);
      }
    } catch (InterruptedException e) {
      log.error("EventsFrameworkStatsPublisher was interrupted...");
      Thread.currentThread().interrupt();
    }
  }

  private void printStreamStat(String streamName, String statName, Object value) {
    log.info("[{}] {}: {}", streamName, statName, value);
  }

  private void printConsumerGroupStat(String streamName, String consumergroupName, String statName, Object value) {
    log.info("[{}][{}] {}: {}", streamName, consumergroupName, statName, value);
  }
}
