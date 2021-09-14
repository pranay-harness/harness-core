/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@TypeAlias("timeoutDetails")
public class TimeoutDetails {
  TimeoutInstance timeoutInstance;
  long expiredAt;

  public TimeoutDetails(TimeoutInstance timeoutInstance) {
    this.timeoutInstance = timeoutInstance;
    this.expiredAt = System.currentTimeMillis();
  }
}
