/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.distribution.barrier;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Forcer {
  private ForcerId id;
  private Map<String, Object> metadata;
  private List<Forcer> children;

  public enum State {
    // The forcer is absent for the barrier purposes
    ABSENT,

    // The forcer is moving forward towards the barrier
    APPROACHING,

    // The forcer arrived at the barrier and apples pushing force to it
    ARRIVED,

    // The forcer abandoned the effort to push the barrier
    ABANDONED,

    // The forcer is abandoned the effort to push the barrier due to timeout
    TIMED_OUT
  }
}
