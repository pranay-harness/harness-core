/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
