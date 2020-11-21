package io.harness.distribution.constraint;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RunnableConsumers {
  private int usedPermits;
  private List<ConsumerId> consumerIds;
}
