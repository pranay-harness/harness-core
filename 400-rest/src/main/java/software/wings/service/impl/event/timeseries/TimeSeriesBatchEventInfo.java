/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
