/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification;

import io.harness.time.Timestamp;

import java.util.SortedMap;
import java.util.TreeMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Vaibhav Tulsyan
 * 13/Oct/2018
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesDataPoint {
  private long timestamp;
  private double value;
  int risk;

  public static SortedMap<Long, TimeSeriesDataPoint> initializeTimeSeriesDataPointsList(
      long startTime, long endTime, long period, int initialValue) {
    SortedMap<Long, TimeSeriesDataPoint> timeSeriesDataPoints = new TreeMap<>();
    for (long i = endTime; i >= startTime; i -= period) {
      timeSeriesDataPoints.put(
          Timestamp.minuteBoundary(i), TimeSeriesDataPoint.builder().timestamp(i).value(initialValue).build());
    }
    return timeSeriesDataPoints;
  }
}
