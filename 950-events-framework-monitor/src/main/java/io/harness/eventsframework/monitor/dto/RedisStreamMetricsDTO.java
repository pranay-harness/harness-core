package io.harness.eventsframework.monitor.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
public class RedisStreamMetricsDTO {
  private RedisStreamDTO redisStreamDTO;
  private long streamSize;
  private double memoryUsageInMBs;
  private double averageMessageSizeInKBs;
  private long deadLetterQueueSize;

  List<RedisStreamConsumerGroupMetricsDTO> consumergroupMetricsDTOs;
}
