/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.dashboard.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeatMapDTO implements Comparable<HeatMapDTO> {
  long startTime;
  long endTime;
  Double riskScore;

  @Override
  public int compareTo(HeatMapDTO o) {
    return Long.compare(this.startTime, o.startTime);
  }
}
