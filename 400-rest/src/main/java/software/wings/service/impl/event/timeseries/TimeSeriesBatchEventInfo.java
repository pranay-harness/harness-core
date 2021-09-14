/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.event.timeseries;

import io.harness.event.model.EventInfo;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TimeSeriesBatchEventInfo implements EventInfo {
  private String accountId;
  private long timestamp;
  private List<DataPoint> dataPointList;

  @Value
  @Builder
  public static class DataPoint {
    private Map<String, Object> data;
  }

  public String getLog() {
    return "TimeSeriesBatchEventInfo{"
        + "accountId='" + accountId + '\'' + ", timestamp=" + timestamp + '}';
  }
}
