package io.harness.eventsframework.impl.redis.monitoring.dto;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACCOUNT_IDENTIFIER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.producer.Message;

@OwnedBy(HarnessTeam.PL)
public class RedisEventMetricDTOMapper {
  public RedisEventMetricDTO prepareRedisEventMetricDTO(Message message) {
    return RedisEventMetricDTO.builder()
        .accountId(message.getMetadataMap().get(ACCOUNT_IDENTIFIER))
        .build();
  }
}
