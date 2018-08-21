package io.harness.distribution.constraint;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Consumer {
  private ConsumerId id;
  private int permits;

  enum State {
    // The consumer is blocked from currently running consumers
    BLOCKED,

    // The currently uses the resource
    RUNNING,

    // The consumer is already done
    FINISHED
  }

  private State state;
}
