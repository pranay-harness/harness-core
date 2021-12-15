package io.harness.eventsframework.impl.redis.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
public class RedisEventDTO {
    private String accountId;
}
