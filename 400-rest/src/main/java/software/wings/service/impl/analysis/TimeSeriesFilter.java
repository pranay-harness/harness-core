/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class TimeSeriesFilter {
  private String cvConfigId;
  private long startTime;
  private long endTime;
  private long historyStartTime;
  private Set<String> txnNames;
  private Set<String> metricNames;
  private Set<String> tags;
}
