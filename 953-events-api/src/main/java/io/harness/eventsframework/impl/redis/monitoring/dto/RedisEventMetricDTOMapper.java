package io.harness.eventsframework.impl.redis.monitoring.dto;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACCOUNT_IDENTIFIER_METRICS_KEY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.producer.Message;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class RedisEventMetricDTOMapper {
  public RedisEventMetricDTO prepareRedisEventMetricDTO(Message message) {
    return RedisEventMetricDTO.builder()
        .accountId(message.getMetadataMap().get(ACCOUNT_IDENTIFIER_METRICS_KEY))
        .build();
  }
}
