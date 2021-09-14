/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
