package service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.monitor.dto.AggregateRedisStreamMetricsDTO;
import io.harness.eventsframework.monitor.dto.RedisStreamConsumerGroupMetricsDTO;
import io.harness.eventsframework.monitor.dto.RedisStreamDTO;
import io.harness.eventsframework.monitor.dto.RedisStreamMetricsDTO;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamGroup;
import org.redisson.api.StreamMessageId;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisConnection;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommands;

@OwnedBy(HarnessTeam.PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class RedisStreamsMetricsAggregatorImpl implements RedisStreamsMetricsAggregator {
  private final RedissonClient redisClient;
  private final RedisClient lowLevelClient;

  private long getStreamLength(RStream<String, String> stream) {
    return stream.size();
  }

  private long getDeadletterQueueLength(RedisStreamDTO redisStreamDTO) {
    String namespace = redisStreamDTO.getNamespace();
    String prefix = namespace.isEmpty() ? "streams:" : namespace + ":streams:";
    RStream<String, String> stream = getRedisStream(prefix + "deadletter_queue:" + redisStreamDTO.getUsecaseName());
    return stream.size();
  }

  private double getTotalMemoryUsage(RedisConnection lowLevelConnection, String streamName) {
    Long bytes = lowLevelConnection.sync(StringCodec.INSTANCE, RedisCommands.MEMORY_USAGE, streamName, "samples", "0");
    return bytes / 1024.0 / 1024.0;
  }

  private int getConsumerBehindCount(
      RStream<String, String> currentStream, StreamMessageId startId, StreamMessageId endId) {
    int batchSize = 500;
    int count = 0;
    while (true) {
      Map<StreamMessageId, Map<String, String>> range = currentStream.range(batchSize, startId, endId);
      int retrievedSize = range.size();
      count += retrievedSize - 1;
      if (retrievedSize <= 1) {
        break;
      }
      StreamMessageId[] objects = range.keySet().toArray(new StreamMessageId[0]);
      startId = objects[objects.length - 1];
    }
    return Math.max(0, count);
  }

  private RStream<String, String> getRedisStream(String streamName) {
    return redisClient.getStream(streamName, new StringCodec("UTF-8"));
  }

  private RedisStreamDTO createRedisStreamDTO(String streamName) {
    String[] streamNameParts = streamName.split(":");
    String namespace = streamNameParts[0].equals("streams") ? "" : streamNameParts[0];
    String usecaseName = streamNameParts[streamNameParts.length - 1];
    return RedisStreamDTO.builder().namespace(namespace).usecaseName(usecaseName).streamName(streamName).build();
  }

  @Override
  public AggregateRedisStreamMetricsDTO getStreamStats() {
    Iterator<String> keysIterator = redisClient.getKeys().getKeysByPattern("*streams:*", 1000).iterator();
    RedisConnection lowLevelConnection = lowLevelClient.connect();

    try {
      List<RedisStreamMetricsDTO> redisStreamMetricsDTOList = new ArrayList<>();
      while (keysIterator.hasNext()) {
        String streamName = keysIterator.next();
        if (streamName.matches("(.*)streams:deadletter_queue(.*)")) {
          log.debug("Skipping deadletter queue key {}", streamName);
          continue;
        }

        RedisStreamDTO redisStreamDTO = createRedisStreamDTO(streamName);
        RStream<String, String> currentStream = getRedisStream(streamName);

        long streamLength = getStreamLength(currentStream);
        double memoryUsageInMBs = getTotalMemoryUsage(lowLevelConnection, streamName);
        double averageMessageSize = 0;

        if (streamLength != 0) {
          averageMessageSize = memoryUsageInMBs * 1024.0 / streamLength;
        }

        long deadletterQueueLength = getDeadletterQueueLength(redisStreamDTO);
        List<StreamGroup> groups = redisClient.getStream(streamName).listGroups();
        StreamMessageId lastGeneratedId = currentStream.getInfo().getLastGeneratedId();
        List<RedisStreamConsumerGroupMetricsDTO> consumerGroupMetricsDTOS = new ArrayList<>();
        for (StreamGroup group : groups) {
          consumerGroupMetricsDTOS.add(
              RedisStreamConsumerGroupMetricsDTO.builder()
                  .consumergroupName(group.getName())
                  .pendingCount(group.getPending())
                  .behindByCount(getConsumerBehindCount(currentStream, group.getLastDeliveredId(), lastGeneratedId))
                  .build());
        }
        redisStreamMetricsDTOList.add(RedisStreamMetricsDTO.builder()
                                          .redisStreamDTO(redisStreamDTO)
                                          .averageMessageSizeInKBs(averageMessageSize)
                                          .deadLetterQueueSize(deadletterQueueLength)
                                          .memoryUsageInMBs(memoryUsageInMBs)
                                          .streamSize(streamLength)
                                          .consumergroupMetricsDTOs(consumerGroupMetricsDTOS)
                                          .build());
      }
      return AggregateRedisStreamMetricsDTO.builder().redisStreamMetricsDTOList(redisStreamMetricsDTOList).build();
    } catch (Exception e) {
      log.error("Failed in getting stream stats", e);
      lowLevelConnection.closeAsync();
      return AggregateRedisStreamMetricsDTO.builder().redisStreamMetricsDTOList(Collections.emptyList()).build();
    }
  }
}