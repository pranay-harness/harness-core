/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
