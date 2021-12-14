package io.harness.eventsframework.monitor.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
public class RedisStreamAccountDTO {
    private String accountId;
}
