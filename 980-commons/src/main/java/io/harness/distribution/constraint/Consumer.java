package io.harness.distribution.constraint;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class Consumer {
  private ConsumerId id;
  private int permits;

  public enum State {
    // The consumer is blocked from currently running consumers
    BLOCKED,

    // The currently uses the resource
    ACTIVE,

    // The consumer is already done
    FINISHED
  }

  private State state;

  private Map<String, Object> context;
}
