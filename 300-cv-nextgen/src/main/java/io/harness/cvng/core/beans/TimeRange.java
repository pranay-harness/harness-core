/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TimeRange {
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC") Instant startTime;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC") Instant endTime;
}
