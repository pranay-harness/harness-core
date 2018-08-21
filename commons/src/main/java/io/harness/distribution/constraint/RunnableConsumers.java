package io.harness.distribution.constraint;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RunnableConsumers {
  private int usedPermits;
  private List<ConsumerId> consumerIds;
}