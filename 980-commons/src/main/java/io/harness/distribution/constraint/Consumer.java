/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.distribution.constraint;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

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
    FINISHED,

    // The consumer is not allowed to take the resource
    REJECTED
  }

  private State state;

  private Map<String, Object> context;
}
